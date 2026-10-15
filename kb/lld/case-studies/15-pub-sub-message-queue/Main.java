import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Main {
    public static void main(String[] args) throws Exception {
        fanOutAndPartitions();
        offsetsAndLag();
        retryAndAtLeastOnce();
        deadLetterQueue();
        rebalance();
        concurrentPublishers();
        validation();
    }

    static void fanOutAndPartitions() throws Exception {
        System.out.println("== Two groups, three partitions ==");
        Broker broker = new Broker(4, new RetryPolicy(3, 100), millis -> { });
        broker.createTopic("orders", 3);
        Recorder seen = new Recorder();
        for (String member : List.of("b1", "b2")) {
            broker.subscribe("orders", "billing", member,
                    m -> seen.add("billing p" + m.partition(), member + ":" + m.payload()),
                    StartPosition.EARLIEST);
        }
        broker.subscribe("orders", "audit", "a1",
                m -> seen.add("audit p" + m.partition(), "a1:" + m.payload()),
                StartPosition.EARLIEST);
        Publisher publisher = new Publisher(broker, "orders");
        List<String> keys = List.of("alice", "bob", "carol", "dave");
        for (int i = 1; i <= 8; i++) {
            publisher.send(keys.get(i % keys.size()), "order-" + i);
        }
        broker.awaitIdle();
        for (String bucket : seen.buckets()) {
            System.out.println(bucket + ": " + seen.get(bucket));
        }
        broker.shutdown();
    }

    static void offsetsAndLag() throws Exception {
        System.out.println("== Offsets, lag, and start position ==");
        Broker broker = new Broker(2, new RetryPolicy(3, 100), millis -> { });
        broker.createTopic("metrics", 1);
        broker.createGroup("metrics", "dashboard", StartPosition.EARLIEST);
        for (int i = 1; i <= 4; i++) {
            broker.publish("metrics", "host-1", "cpu-" + i);
        }
        System.out.println("lag while the group has no member: "
                + broker.lag("metrics", "dashboard"));
        Recorder seen = new Recorder();
        MessageHandler dashboard = m -> seen.add("dashboard", m.payload());
        broker.subscribe("metrics", "dashboard", "d1", dashboard, StartPosition.EARLIEST);
        broker.awaitIdle();
        System.out.println("lag after the member joins: " + broker.lag("metrics", "dashboard")
                + ", committed offsets: " + broker.committed("metrics", "dashboard"));

        broker.subscribe("metrics", "replay", "r1",
                m -> seen.add("replay", m.payload()), StartPosition.EARLIEST);
        broker.subscribe("metrics", "latest-only", "l1",
                m -> seen.add("latest-only", m.payload()), StartPosition.LATEST);
        broker.publish("metrics", "host-1", "cpu-5");
        broker.publish("metrics", "host-1", "cpu-6");
        broker.awaitIdle();
        for (String group : List.of("dashboard", "replay", "latest-only")) {
            System.out.println(group + " received " + seen.get(group));
        }
        broker.shutdown();
    }

    static void retryAndAtLeastOnce() throws Exception {
        System.out.println("== Retry with backoff: at-least-once delivery ==");
        List<Long> delays = Collections.synchronizedList(new ArrayList<>());
        Broker broker = new Broker(2, new RetryPolicy(3, 100), delays::add);
        broker.createTopic("emails", 1);
        AtomicInteger calls = new AtomicInteger();
        Set<String> effects = ConcurrentHashMap.newKeySet();
        broker.subscribe("emails", "sender", "s1", m -> {
            int call = calls.incrementAndGet();
            effects.add("sent " + m.id());   // idempotent write: the same id, the same entry
            if (call <= 2) {
                throw new IllegalStateException("smtp timeout");
            }
        }, StartPosition.EARLIEST);
        broker.publish("emails", "user-1", "welcome");
        broker.awaitIdle();
        System.out.println("handler calls for one message: " + calls.get());
        System.out.println("backoff delays in ms: " + delays);
        System.out.println("distinct effects: " + effects + ", lag: "
                + broker.lag("emails", "sender"));
        broker.shutdown();
    }

    static void deadLetterQueue() throws Exception {
        System.out.println("== Dead-letter queue ==");
        List<Long> delays = Collections.synchronizedList(new ArrayList<>());
        Broker broker = new Broker(2, new RetryPolicy(3, 50), delays::add);
        broker.createTopic("payments", 1);
        Recorder seen = new Recorder();
        broker.subscribe("payments", "ledger", "l1", m -> {
            if (m.payload().startsWith("bad")) {
                throw new IllegalArgumentException("cannot parse " + m.payload());
            }
            seen.add("ledger", m.payload());
        }, StartPosition.EARLIEST);
        broker.subscribe("payments.dlq", "ops", "o1", m -> {
            seen.add("dead letter", m.payload());
            new TreeMap<>(m.headers()).forEach((k, v) -> seen.add("header", k + " = " + v));
        }, StartPosition.EARLIEST);
        broker.publish("payments", "acc-1", "pay-1");
        broker.publish("payments", "acc-1", "bad-2");
        broker.publish("payments", "acc-1", "pay-3");
        broker.awaitIdle();
        System.out.println("ledger processed: " + seen.get("ledger"));
        System.out.println("dead letters: " + seen.get("dead letter"));
        seen.get("header").forEach(header -> System.out.println("  header " + header));
        System.out.println("backoff delays in ms: " + delays + ", lag: "
                + broker.lag("payments", "ledger"));
        broker.shutdown();
    }

    static void rebalance() throws Exception {
        System.out.println("== Members join and leave ==");
        Broker broker = new Broker(4, new RetryPolicy(3, 100), millis -> { });
        broker.createTopic("jobs", 4);
        Recorder seen = new Recorder();
        for (String member : List.of("m1", "m2")) {
            broker.subscribe("jobs", "workers", member,
                    m -> seen.add(member, m.payload()), StartPosition.EARLIEST);
        }
        System.out.println("two members: " + broker.assignment("jobs", "workers"));
        broker.subscribe("jobs", "workers", "m3", m -> seen.add("m3", m.payload()),
                StartPosition.EARLIEST);
        System.out.println("three members: " + broker.assignment("jobs", "workers"));
        broker.unsubscribe("jobs", "workers", "m2");
        broker.unsubscribe("jobs", "workers", "m3");
        System.out.println("one member: " + broker.assignment("jobs", "workers"));
        for (int i = 0; i < 12; i++) {
            broker.publish("jobs", "key-" + i, "job-" + i);
        }
        broker.awaitIdle();
        System.out.println("m1 handled " + seen.get("m1").size() + " of 12, lag: "
                + broker.lag("jobs", "workers"));
        broker.shutdown();
    }

    static void concurrentPublishers() throws Exception {
        System.out.println("== Four publisher threads, 250 messages each ==");
        Broker broker = new Broker(4, new RetryPolicy(3, 100), millis -> { });
        broker.createTopic("stream", 4);
        Map<String, List<Integer>> byKey = new ConcurrentHashMap<>();
        for (String member : List.of("c1", "c2")) {
            broker.subscribe("stream", "g", member, m -> byKey
                    .computeIfAbsent(m.key(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(Integer.parseInt(m.payload())), StartPosition.EARLIEST);
        }
        List<Thread> threads = new ArrayList<>();
        for (int p = 0; p < 4; p++) {
            Publisher publisher = new Publisher(broker, "stream");
            String key = "publisher-" + p;
            Thread thread = new Thread(() -> {
                for (int seq = 0; seq < 250; seq++) {
                    publisher.send(key, String.valueOf(seq));
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        broker.awaitIdle();
        int delivered = 0;
        boolean ordered = true;
        for (List<Integer> sequence : byKey.values()) {
            delivered += sequence.size();
            for (int i = 0; i < sequence.size(); i++) {
                ordered &= sequence.get(i) == i;
            }
        }
        System.out.println("delivered " + delivered + " of 1000, lag: "
                + broker.lag("stream", "g"));
        System.out.println("messages of each publisher arrived in order: " + ordered);
        broker.shutdown();
    }

    static void validation() throws Exception {
        System.out.println("== Validation ==");
        Broker broker = new Broker(1, new RetryPolicy(1, 0), millis -> { });
        List<Runnable> badCalls = List.of(
                () -> broker.createTopic("empty", 0),
                () -> broker.publish("nope", "k", "v"),
                () -> new RetryPolicy(0, 10));
        for (Runnable call : badCalls) {
            try {
                call.run();
            } catch (IllegalArgumentException e) {
                System.out.println("rejected: " + e.getMessage());
            }
        }
        broker.shutdown();
    }
}

// Demo helper: collects lines from handler threads. Each bucket is a synchronized list.
final class Recorder {
    private final Map<String, List<String>> lines = new ConcurrentHashMap<>();

    void add(String bucket, String line) {
        lines.computeIfAbsent(bucket, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(line);
    }

    List<String> get(String bucket) {
        List<String> list = lines.getOrDefault(bucket, List.of());
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    Set<String> buckets() {
        return new TreeSet<>(lines.keySet());
    }
}

record Message(String topic, int partition, long offset, String key, String payload,
               Map<String, String> headers) {
    Message {
        headers = Map.copyOf(headers);
    }

    String id() {
        return topic + "-" + partition + "-" + offset;
    }
}

@FunctionalInterface
interface MessageHandler {
    void handle(Message message) throws Exception;
}

@FunctionalInterface
interface Partitioner {
    int partitionFor(String key, int partitionCount);

    static Partitioner byKeyHash() {
        return (key, count) -> Math.floorMod(key.hashCode(), count);
    }
}

@FunctionalInterface
interface Sleeper {
    void sleep(long millis) throws InterruptedException;
}

enum StartPosition { EARLIEST, LATEST }

record RetryPolicy(int maxAttempts, long baseDelayMillis) {
    RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        if (baseDelayMillis < 0) {
            throw new IllegalArgumentException("baseDelayMillis must not be negative");
        }
    }

    // Exponential backoff: the delay doubles after each failed attempt.
    long delayAfterFailure(int failedAttempts) {
        return baseDelayMillis << (failedAttempts - 1);
    }
}

final class Partition {
    private final String topic;
    private final int id;
    private final List<Message> log = new ArrayList<>();

    Partition(String topic, int id) {
        this.topic = topic;
        this.id = id;
    }

    // The offset of a message is its index in the log. The lock makes append and size atomic.
    synchronized Message append(String key, String payload, Map<String, String> headers) {
        Message message = new Message(topic, id, log.size(), key, payload, headers);
        log.add(message);
        return message;
    }

    synchronized Optional<Message> read(long offset) {
        return offset < log.size() ? Optional.of(log.get((int) offset)) : Optional.empty();
    }

    synchronized long endOffset() {
        return log.size();
    }
}

final class Topic {
    private final String name;
    private final Partition[] partitions;
    private final Partitioner partitioner;

    Topic(String name, int partitionCount, Partitioner partitioner) {
        this.name = name;
        this.partitioner = partitioner;
        this.partitions = new Partition[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            partitions[i] = new Partition(name, i);
        }
    }

    Message append(String key, String payload, Map<String, String> headers) {
        Objects.requireNonNull(key, "key");
        int index = partitioner.partitionFor(key, partitions.length);
        return partitions[index].append(key, payload, headers);
    }

    Partition partition(int index) {
        return partitions[index];
    }

    int partitionCount() {
        return partitions.length;
    }
}

final class Publisher {
    private final Broker broker;
    private final String topic;

    Publisher(Broker broker, String topic) {
        this.broker = broker;
        this.topic = topic;
    }

    Message send(String key, String payload) {
        return broker.publish(topic, key, payload);
    }
}

// The read position of one group in one partition. Only the drain task of the partition writes it.
final class PartitionCursor {
    final AtomicBoolean running = new AtomicBoolean();
    volatile long committed;

    PartitionCursor(long start) {
        this.committed = start;
    }
}

final class ConsumerGroup {
    private record Member(String id, MessageHandler handler) { }

    private final String name;
    private final Topic topic;
    private final Broker broker;
    private final PartitionCursor[] cursors;
    private final List<Member> members = new ArrayList<>();   // guarded by this
    private volatile Map<Integer, Member> assignment = Map.of();

    ConsumerGroup(String name, Topic topic, StartPosition start, Broker broker) {
        this.name = name;
        this.topic = topic;
        this.broker = broker;
        this.cursors = new PartitionCursor[topic.partitionCount()];
        for (int p = 0; p < cursors.length; p++) {
            long startOffset = start == StartPosition.EARLIEST ? 0 : topic.partition(p).endOffset();
            cursors[p] = new PartitionCursor(startOffset);
        }
    }

    synchronized void join(String memberId, MessageHandler handler) {
        for (Member member : members) {
            if (member.id().equals(memberId)) {
                throw new IllegalArgumentException("duplicate member: " + memberId);
            }
        }
        members.add(new Member(memberId, handler));
        rebalance();
    }

    synchronized void leave(String memberId) {
        if (!members.removeIf(member -> member.id().equals(memberId))) {
            throw new IllegalArgumentException("unknown member: " + memberId);
        }
        rebalance();
    }

    // Partition p goes to member p mod n. The caller holds the group lock.
    private void rebalance() {
        Map<Integer, Member> next = new HashMap<>();
        for (int p = 0; p < cursors.length && !members.isEmpty(); p++) {
            next.put(p, members.get(p % members.size()));
        }
        assignment = Map.copyOf(next);
        for (int p = 0; p < cursors.length; p++) {
            kick(p);
        }
    }

    // Starts a drain task for the partition unless one already runs. At most one task runs for
    // each partition, so the messages of a partition reach the handler in offset order.
    void kick(int partition) {
        PartitionCursor cursor = cursors[partition];
        if (cursor.running.compareAndSet(false, true)) {
            try {
                broker.submitDrain(() -> drain(partition));
            } catch (RejectedExecutionException e) {
                cursor.running.set(false);   // the broker is shut down
            }
        }
    }

    private void drain(int partition) {
        PartitionCursor cursor = cursors[partition];
        try {
            while (true) {
                Member member = assignment.get(partition);
                if (member == null) {
                    return;
                }
                Optional<Message> next = topic.partition(partition).read(cursor.committed);
                if (next.isEmpty()) {
                    return;
                }
                deliver(member, next.get());
                cursor.committed = next.get().offset() + 1;   // commit after the handler finished
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // the message stays uncommitted
        } finally {
            cursor.running.set(false);
            if (hasWork(partition) && !Thread.currentThread().isInterrupted()) {
                kick(partition);   // a message or a member arrived after the last check
            }
        }
    }

    private void deliver(Member member, Message message) throws InterruptedException {
        RetryPolicy policy = broker.retryPolicy();
        Exception lastError = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                member.handler().handle(message);
                return;
            } catch (Exception e) {
                lastError = e;
                if (attempt < policy.maxAttempts()) {
                    broker.sleeper().sleep(policy.delayAfterFailure(attempt));
                }
            }
        }
        broker.deadLetter(message, name, policy.maxAttempts(), lastError);
    }

    private boolean hasWork(int partition) {
        return assignment.get(partition) != null
                && cursors[partition].committed < topic.partition(partition).endOffset();
    }

    long lag() {
        long lag = 0;
        for (int p = 0; p < cursors.length; p++) {
            lag += topic.partition(p).endOffset() - cursors[p].committed;
        }
        return lag;
    }

    List<Long> committedOffsets() {
        List<Long> offsets = new ArrayList<>();
        for (PartitionCursor cursor : cursors) {
            offsets.add(cursor.committed);
        }
        return offsets;
    }

    Map<Integer, String> assignmentView() {
        Map<Integer, String> view = new TreeMap<>();
        assignment.forEach((partition, member) -> view.put(partition, member.id()));
        return view;
    }
}

record GroupKey(String topic, String group) { }

final class Broker {
    private static final String DLQ_SUFFIX = ".dlq";

    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final Map<GroupKey, ConsumerGroup> groups = new ConcurrentHashMap<>();
    private final Map<String, List<ConsumerGroup>> groupsByTopic = new ConcurrentHashMap<>();
    private final ExecutorService workers;
    private final RetryPolicy retryPolicy;
    private final Sleeper sleeper;
    private final Partitioner partitioner;
    private final Object idleLock = new Object();
    private int inFlight;   // drain tasks that are queued or running. Guarded by idleLock.

    Broker(int workerThreads, RetryPolicy retryPolicy, Sleeper sleeper) {
        this(workerThreads, retryPolicy, sleeper, Partitioner.byKeyHash());
    }

    Broker(int workerThreads, RetryPolicy retryPolicy, Sleeper sleeper, Partitioner partitioner) {
        this.workers = Executors.newFixedThreadPool(workerThreads, task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        });
        this.retryPolicy = retryPolicy;
        this.sleeper = sleeper;
        this.partitioner = partitioner;
    }

    RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    Sleeper sleeper() {
        return sleeper;
    }

    // Creates the topic and its dead-letter topic. A second call with the same name does nothing.
    void createTopic(String name, int partitionCount) {
        if (partitionCount < 1) {
            throw new IllegalArgumentException("partitions must be positive");
        }
        topics.computeIfAbsent(name, n -> new Topic(n, partitionCount, partitioner));
        if (!name.endsWith(DLQ_SUFFIX)) {
            createTopic(name + DLQ_SUFFIX, 1);
        }
    }

    Message publish(String topicName, String key, String payload) {
        return publish(topicName, key, payload, Map.of());
    }

    Message publish(String topicName, String key, String payload, Map<String, String> headers) {
        Message message = topicOrThrow(topicName).append(key, payload, headers);
        for (ConsumerGroup group : groupsByTopic.getOrDefault(topicName, List.of())) {
            group.kick(message.partition());
        }
        return message;
    }

    void createGroup(String topicName, String groupName, StartPosition start) {
        Topic topic = topicOrThrow(topicName);
        groups.computeIfAbsent(new GroupKey(topicName, groupName), key -> {
            ConsumerGroup group = new ConsumerGroup(groupName, topic, start, this);
            groupsByTopic.computeIfAbsent(topicName, t -> new CopyOnWriteArrayList<>()).add(group);
            return group;
        });
    }

    // The start position counts only when this call creates the group.
    void subscribe(String topicName, String groupName, String memberId, MessageHandler handler,
                   StartPosition start) {
        createGroup(topicName, groupName, start);
        groupOrThrow(topicName, groupName).join(memberId, handler);
    }

    void unsubscribe(String topicName, String groupName, String memberId) {
        groupOrThrow(topicName, groupName).leave(memberId);
    }

    long lag(String topicName, String groupName) {
        return groupOrThrow(topicName, groupName).lag();
    }

    List<Long> committed(String topicName, String groupName) {
        return groupOrThrow(topicName, groupName).committedOffsets();
    }

    Map<Integer, String> assignment(String topicName, String groupName) {
        return groupOrThrow(topicName, groupName).assignmentView();
    }

    // A message that failed all attempts goes to "<topic>.dlq".
    // A dead letter that fails is dropped.
    void deadLetter(Message message, String group, int attempts, Exception error) {
        if (message.topic().endsWith(DLQ_SUFFIX)) {
            return;
        }
        Map<String, String> headers = Map.of(
                "source", message.id(),
                "group", group,
                "attempts", String.valueOf(attempts),
                "error", error.getClass().getSimpleName() + ": " + error.getMessage());
        publish(message.topic() + DLQ_SUFFIX, message.key(), message.payload(), headers);
    }

    void submitDrain(Runnable drain) {
        synchronized (idleLock) {
            inFlight++;
        }
        try {
            workers.execute(() -> {
                try {
                    drain.run();
                } finally {
                    finishDrain();
                }
            });
        } catch (RejectedExecutionException e) {
            finishDrain();
            throw e;
        }
    }

    private void finishDrain() {
        synchronized (idleLock) {
            if (--inFlight == 0) {
                idleLock.notifyAll();
            }
        }
    }

    // Waits until no drain task is queued or running. A task that publishes a new message
    // starts its follow-up task before it ends, so the count never drops to 0 too early.
    void awaitIdle() throws InterruptedException {
        synchronized (idleLock) {
            while (inFlight > 0) {
                idleLock.wait();
            }
        }
    }

    void shutdown() throws InterruptedException {
        workers.shutdown();
        workers.awaitTermination(10, TimeUnit.SECONDS);
    }

    private Topic topicOrThrow(String name) {
        Topic topic = topics.get(name);
        if (topic == null) {
            throw new IllegalArgumentException("unknown topic: " + name);
        }
        return topic;
    }

    private ConsumerGroup groupOrThrow(String topicName, String groupName) {
        ConsumerGroup group = groups.get(new GroupKey(topicName, groupName));
        if (group == null) {
            throw new IllegalArgumentException("unknown group: " + groupName);
        }
        return group;
    }
}
