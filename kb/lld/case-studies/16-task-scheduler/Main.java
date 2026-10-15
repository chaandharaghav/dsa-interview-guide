import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class Main {
    public static void main(String[] args) throws Exception {
        delayedTasksRunInDueOrder();
        recurringTaskAndCancellation();
        fixedRateAgainstFixedDelay();
        workersRunTasksInParallel();
        failureDoesNotStopRecurringTask();
        concurrentScheduling();
        shutdownAndValidation();
    }

    // Moves the manual clock to time t, then waits until every due task has finished.
    static void advanceTo(ManualTimeSource clock, TaskScheduler scheduler, long t)
            throws InterruptedException {
        clock.advance(t - clock.nowMillis());
        scheduler.awaitIdle();
    }

    static void delayedTasksRunInDueOrder() throws Exception {
        System.out.println("== One-time tasks run in due order ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(1, clock);
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        record Job(String name, long delayMillis) { }
        for (Job job : List.of(new Job("report", 300), new Job("email", 100),
                new Job("cleanup", 200), new Job("audit", 200))) {
            Runnable action = () -> log.add(job.name() + "@" + clock.nowMillis());
            scheduler.schedule(job.name(), action, job.delayMillis());
        }
        System.out.println("pending: " + scheduler.pendingCount() + ", executed: " + log);
        for (long t : new long[] {50, 100, 200, 300}) {
            advanceTo(clock, scheduler, t);
            System.out.println("t=" + t + " pending: " + scheduler.pendingCount()
                    + ", executed: " + log);
        }
        scheduler.shutdown();
    }

    static void recurringTaskAndCancellation() throws Exception {
        System.out.println("== Recurring task and cancellation ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(2, clock);
        AtomicInteger beats = new AtomicInteger();
        TaskHandle heartbeat = scheduler.scheduleAtFixedRate(
                "heartbeat", beats::incrementAndGet, 100, 100);
        for (long t : new long[] {100, 200, 300}) {
            advanceTo(clock, scheduler, t);
            System.out.println("t=" + t + " heartbeats: " + beats.get());
        }
        System.out.println("cancel returns: " + heartbeat.cancel());
        advanceTo(clock, scheduler, 600);
        System.out.println("t=600 heartbeats: " + beats.get() + ", state: " + heartbeat.state());
        System.out.println("second cancel returns: " + heartbeat.cancel());

        AtomicInteger ran = new AtomicInteger();
        TaskHandle lost = scheduler.schedule("lost", ran::incrementAndGet, 400);
        System.out.println("pending before cancel: " + scheduler.pendingCount());
        System.out.println("cancel returns: " + lost.cancel());
        advanceTo(clock, scheduler, 1_000);
        System.out.println("pending: " + scheduler.pendingCount() + ", runs: " + ran.get()
                + ", state: " + lost.state());
        scheduler.shutdown();
    }

    static void fixedRateAgainstFixedDelay() throws Exception {
        System.out.println("== Fixed rate and fixed delay after a pause of 350 ms ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(2, clock);
        AtomicInteger rate = new AtomicInteger();
        AtomicInteger delay = new AtomicInteger();
        scheduler.scheduleAtFixedRate("rate", rate::incrementAndGet, 100, 100);
        scheduler.scheduleWithFixedDelay("delay", delay::incrementAndGet, 100, 100);
        advanceTo(clock, scheduler, 350);
        System.out.println("t=350 fixed rate ran " + rate.get()
                + ", fixed delay ran " + delay.get());
        advanceTo(clock, scheduler, 450);
        System.out.println("t=450 fixed rate ran " + rate.get()
                + ", fixed delay ran " + delay.get());
        scheduler.shutdown();
    }

    static void workersRunTasksInParallel() throws Exception {
        System.out.println("== Three workers run three due tasks at the same time ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(3, clock);
        CyclicBarrier barrier = new CyclicBarrier(3);
        AtomicInteger met = new AtomicInteger();
        Runnable meet = () -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);   // all three must be inside at once
                met.incrementAndGet();
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                // the tasks did not run in parallel: nobody counts
            }
        };
        for (int i = 1; i <= 3; i++) {
            scheduler.schedule("meet-" + i, meet, 500);
        }
        advanceTo(clock, scheduler, 500);
        System.out.println("tasks that passed the barrier: " + met.get());
        scheduler.shutdown();
    }

    static void failureDoesNotStopRecurringTask() throws Exception {
        System.out.println("== A failing run does not stop the schedule ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(1, clock);
        AtomicInteger calls = new AtomicInteger();
        TaskHandle flaky = scheduler.scheduleAtFixedRate("flaky", () -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("boom");
            }
        }, 100, 100);
        for (long t : new long[] {100, 200, 300}) {
            advanceTo(clock, scheduler, t);
        }
        System.out.println("runs: " + flaky.runCount() + ", failures: " + flaky.failureCount()
                + ", state: " + flaky.state());
        scheduler.shutdown();
    }

    static void concurrentScheduling() throws Exception {
        System.out.println("== Four threads schedule 250 tasks each ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(4, clock);
        AtomicInteger executed = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            int owner = t;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 250; j++) {
                    scheduler.schedule("job-" + owner + "-" + j, executed::incrementAndGet,
                            (j % 10 + 1) * 10);
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("pending: " + scheduler.pendingCount()
                + ", executed: " + executed.get());
        advanceTo(clock, scheduler, 100);
        System.out.println("t=100 pending: " + scheduler.pendingCount()
                + ", executed: " + executed.get());
        scheduler.shutdown();
    }

    static void shutdownAndValidation() throws Exception {
        System.out.println("== Shutdown and validation ==");
        ManualTimeSource clock = new ManualTimeSource();
        TaskScheduler scheduler = TaskScheduler.start(1, clock);
        for (int i = 1; i <= 3; i++) {
            scheduler.schedule("later-" + i, () -> { }, 1_000 * i);
        }
        System.out.println("discarded by shutdown: " + scheduler.shutdown());
        List<Runnable> badCalls = List.of(
                () -> scheduler.schedule("late", () -> { }, 10),
                () -> scheduler.schedule("neg", () -> { }, -1),
                () -> Recurrence.fixedRate(0));
        for (Runnable call : badCalls) {
            try {
                call.run();
            } catch (IllegalStateException | IllegalArgumentException e) {
                System.out.println("rejected: " + e.getMessage());
            }
        }
    }
}

// The scheduler reads time and waits through this interface, so a demo can move time by hand.
interface TimeSource {
    long nowMillis();

    // Waits on the condition for at most maxMillis of this source's time.
    // The caller holds the lock of the condition.
    void awaitSignal(Condition condition, long maxMillis) throws InterruptedException;

    // The source calls the listener when time changes without a real wait.
    default void onChange(Runnable listener) { }
}

final class SystemTimeSource implements TimeSource {
    @Override
    public long nowMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());   // monotonic
    }

    @Override
    public void awaitSignal(Condition condition, long maxMillis) throws InterruptedException {
        condition.await(maxMillis, TimeUnit.MILLISECONDS);
    }
}

final class ManualTimeSource implements TimeSource {
    private final AtomicLong now = new AtomicLong();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    @Override
    public long nowMillis() {
        return now.get();
    }

    // Time passes only when advance() runs, so the wait ends only on a signal.
    @Override
    public void awaitSignal(Condition condition, long maxMillis) throws InterruptedException {
        condition.await();
    }

    @Override
    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    void advance(long millis) {
        now.addAndGet(millis);
        listeners.forEach(Runnable::run);
    }
}

// Strategy: the rule that decides when a task runs again.
@FunctionalInterface
interface Recurrence {
    Recurrence ONCE = (scheduledAt, finishedAt) -> OptionalLong.empty();

    OptionalLong nextRun(long scheduledAt, long finishedAt);

    static Recurrence fixedRate(long periodMillis) {
        if (periodMillis <= 0) {
            throw new IllegalArgumentException("period must be positive");
        }
        return (scheduledAt, finishedAt) -> OptionalLong.of(scheduledAt + periodMillis);
    }

    static Recurrence fixedDelay(long delayMillis) {
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("delay must be positive");
        }
        return (scheduledAt, finishedAt) -> OptionalLong.of(finishedAt + delayMillis);
    }
}

enum TaskState { SCHEDULED, RUNNING, DONE, CANCELLED }

interface TaskHandle {
    long id();

    String name();

    TaskState state();

    int runCount();

    int failureCount();

    // True if this call stops the task from starting another run.
    boolean cancel();
}

final class ScheduledTask implements TaskHandle {
    private final long id;
    private final String name;
    private final TaskScheduler scheduler;
    final Runnable action;
    final Recurrence recurrence;
    // The fields below change only while the scheduler lock is held.
    long nextRunAt;
    long sequence;
    volatile TaskState state = TaskState.SCHEDULED;
    volatile int runs;
    volatile int failures;

    ScheduledTask(long id, String name, Runnable action, Recurrence recurrence, long firstRunAt,
                  TaskScheduler scheduler) {
        this.id = id;
        this.name = name;
        this.action = action;
        this.recurrence = recurrence;
        this.nextRunAt = firstRunAt;
        this.scheduler = scheduler;
    }

    boolean repeats() {
        return recurrence != Recurrence.ONCE;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public TaskState state() {
        return state;
    }

    @Override
    public int runCount() {
        return runs;
    }

    @Override
    public int failureCount() {
        return failures;
    }

    @Override
    public boolean cancel() {
        return scheduler.cancel(this);
    }
}

final class TaskScheduler {
    private final TimeSource time;
    private final ExecutorService workers;
    private final Thread dispatcher;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wakeup = lock.newCondition();   // the dispatcher waits here
    private final Condition idle = lock.newCondition();     // awaitIdle() waits here
    // The head is the task with the earliest run time. Ties go to the task queued first.
    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>(
            Comparator.comparingLong((ScheduledTask t) -> t.nextRunAt)
                    .thenComparingLong(t -> t.sequence));
    private long nextId;
    private long nextSequence;
    private int running;
    private boolean shutdown;

    private TaskScheduler(int workerThreads, TimeSource time) {
        this.time = time;
        this.workers = Executors.newFixedThreadPool(workerThreads, task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        });
        this.dispatcher = new Thread(this::dispatchLoop);
        this.dispatcher.setDaemon(true);
    }

    static TaskScheduler start(int workerThreads, TimeSource time) {
        TaskScheduler scheduler = new TaskScheduler(workerThreads, time);
        time.onChange(scheduler::wakeDispatcher);
        scheduler.dispatcher.start();
        return scheduler;
    }

    TaskHandle schedule(String name, Runnable action, long delayMillis) {
        return add(name, action, delayMillis, Recurrence.ONCE);
    }

    TaskHandle scheduleAtFixedRate(String name, Runnable action, long initialDelayMillis,
                                   long periodMillis) {
        return add(name, action, initialDelayMillis, Recurrence.fixedRate(periodMillis));
    }

    TaskHandle scheduleWithFixedDelay(String name, Runnable action, long initialDelayMillis,
                                      long delayMillis) {
        return add(name, action, initialDelayMillis, Recurrence.fixedDelay(delayMillis));
    }

    private TaskHandle add(String name, Runnable action, long delayMillis, Recurrence recurrence) {
        Objects.requireNonNull(action, "action");
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        lock.lock();
        try {
            if (shutdown) {
                throw new IllegalStateException("scheduler is shut down");
            }
            ScheduledTask task = new ScheduledTask(
                    nextId++, name, action, recurrence, time.nowMillis() + delayMillis, this);
            enqueue(task);
            return task;
        } finally {
            lock.unlock();
        }
    }

    // The caller holds the lock.
    private void enqueue(ScheduledTask task) {
        task.sequence = nextSequence++;
        task.state = TaskState.SCHEDULED;
        queue.add(task);
        if (queue.peek() == task) {
            wakeup.signalAll();   // the dispatcher must wait for this earlier time
        }
    }

    // The dispatcher is the only thread that moves tasks from the queue to the workers.
    private void dispatchLoop() {
        lock.lock();
        try {
            while (!shutdown) {
                ScheduledTask head = queue.peek();
                if (head == null) {
                    wakeup.await();
                    continue;
                }
                long delay = head.nextRunAt - time.nowMillis();
                if (delay > 0) {
                    time.awaitSignal(wakeup, delay);
                    continue;
                }
                queue.poll();
                head.state = TaskState.RUNNING;
                running++;
                workers.execute(() -> runTask(head));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void runTask(ScheduledTask task) {
        boolean failed = true;
        try {
            task.action.run();
            failed = false;
        } catch (RuntimeException e) {
            // The failure is counted below. The schedule goes on.
        } finally {
            complete(task, failed);
        }
    }

    private void complete(ScheduledTask task, boolean failed) {
        lock.lock();
        try {
            task.runs++;
            if (failed) {
                task.failures++;
            }
            running--;
            if (task.state == TaskState.RUNNING) {
                OptionalLong next = shutdown ? OptionalLong.empty()
                        : task.recurrence.nextRun(task.nextRunAt, time.nowMillis());
                if (next.isPresent()) {
                    task.nextRunAt = next.getAsLong();
                    enqueue(task);
                } else {
                    task.state = TaskState.DONE;
                }
            }
            idle.signalAll();
        } finally {
            lock.unlock();
        }
    }

    boolean cancel(ScheduledTask task) {
        lock.lock();
        try {
            TaskState state = task.state;
            if (state == TaskState.DONE || state == TaskState.CANCELLED) {
                return false;
            }
            if (state == TaskState.RUNNING && !task.repeats()) {
                return false;   // the only run has started, and the scheduler does not interrupt it
            }
            if (state == TaskState.SCHEDULED) {
                queue.remove(task);   // O(n): the price of an exact pending count
            }
            task.state = TaskState.CANCELLED;
            idle.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    int pendingCount() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    // Waits until no task runs and no task is due. A demo calls it after it moves the clock.
    void awaitIdle() throws InterruptedException {
        lock.lock();
        try {
            while (running > 0 || hasDueTask()) {
                idle.await();
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean hasDueTask() {
        ScheduledTask head = queue.peek();
        return head != null && head.nextRunAt <= time.nowMillis();
    }

    private void wakeDispatcher() {
        lock.lock();
        try {
            wakeup.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Discards the pending tasks, lets the running tasks finish, and returns the discard count.
    int shutdown() throws InterruptedException {
        int discarded;
        lock.lock();
        try {
            shutdown = true;
            discarded = queue.size();
            for (ScheduledTask task : queue) {
                task.state = TaskState.CANCELLED;
            }
            queue.clear();
            wakeup.signalAll();
            idle.signalAll();
        } finally {
            lock.unlock();
        }
        dispatcher.join();
        workers.shutdown();
        workers.awaitTermination(10, TimeUnit.SECONDS);
        return discarded;
    }
}
