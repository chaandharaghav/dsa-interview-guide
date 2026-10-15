import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Clock clock = new TickingClock(Instant.parse("2026-01-01T00:00:00Z"));
        // The demo gives the file appender a StringWriter, so it does not touch the disk.
        StringWriter fileSink = new StringWriter();
        InMemoryAppender alerts = new InMemoryAppender(new JsonFormatter());

        // Chain: console (INFO and above) -> file (WARN and above) -> alerts (ERROR only).
        LogHandler console = new LogHandler(Level.INFO,
                new ConsoleAppender(System.out, new SimpleFormatter()));
        LogHandler file = new LogHandler(Level.WARN,
                new FileAppender(fileSink, new SimpleFormatter()));
        LogHandler alert = new LogHandler(Level.ERROR, alerts);
        console.setNext(file).setNext(alert);

        LogManager manager = new LogManager(clock, Level.INFO, console);
        Logger orders = manager.getLogger("OrderService");
        Logger payments = manager.getLogger("Payments");

        System.out.println("== 1. Levels and the handler chain ==");
        orders.debug("cart loaded");
        orders.info("order 1001 created");
        payments.warn("card gateway is slow");
        payments.error("payment declined");
        System.out.println("same logger for the same name: "
                + (manager.getLogger("OrderService") == orders));

        System.out.println("== 2. Lazy messages ==");
        AtomicInteger built = new AtomicInteger();
        orders.log(Level.DEBUG, () -> "expensive debug text " + built.incrementAndGet());
        System.out.println("debug message built while DEBUG is off: " + built.get());
        orders.setLevel(Level.DEBUG);
        orders.log(Level.DEBUG, () -> "expensive debug text " + built.incrementAndGet());
        System.out.println("debug message built while DEBUG is on: " + built.get());
        orders.setLevel(Level.INFO);

        System.out.println("== 3. A failing appender does not break the application ==");
        LogHandler broken = new LogHandler(Level.INFO, new FailingAppender());
        LogHandler last = new LogHandler(Level.INFO, new ConsoleAppender(System.out,
                new SimpleFormatter()));
        broken.setNext(last);
        Logger risky = new LogManager(clock, Level.INFO, broken).getLogger("Risky");
        risky.info("still delivered to the next handler");
        System.out.println("failures counted by the broken handler: " + broken.failures());

        System.out.println("== 4. File and alert appenders ==");
        manager.close();
        System.out.println("file lines:");
        fileSink.toString().lines().forEach(line -> System.out.println("  " + line));
        System.out.println("alert lines:");
        alerts.lines().forEach(line -> System.out.println("  " + line));

        System.out.println("== 5. Async appender ==");
        InMemoryAppender target = new InMemoryAppender(new SimpleFormatter());
        AsyncAppender async = new AsyncAppender(target, 8);
        LogManager asyncManager = new LogManager(clock, Level.INFO, new LogHandler(Level.INFO,
                async));
        Logger audit = asyncManager.getLogger("Audit");
        for (int i = 1; i <= 3; i++) {
            audit.info("event " + i);
        }
        int threads = 4;
        int perThread = 50;
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int id = t;
            Thread worker = new Thread(() -> {
                Logger log = asyncManager.getLogger("Worker" + id);
                for (int i = 0; i < perThread; i++) {
                    log.info("msg " + i);
                }
            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        asyncManager.close();
        List<String> lines = target.lines();
        System.out.println("lines delivered after close: " + lines.size());
        System.out.println("first three:");
        lines.subList(0, 3).forEach(line -> System.out.println("  " + line));
        System.out.println("per-thread order kept: "
                + perThreadOrderKept(lines, threads, perThread));
        System.out.println("failures in the async worker: " + async.failures());

        System.out.println("== 6. Global logger (Singleton) and its cost ==");
        Clock globalClock = new TickingClock(Instant.parse("2026-02-01T00:00:00Z"));
        GlobalLogging.install(new LogManager(globalClock, Level.INFO, new LogHandler(Level.INFO,
                new ConsoleAppender(System.out, new SimpleFormatter()))));
        GlobalLogging.logger("Legacy").info("called through the global holder");
    }

    private static boolean perThreadOrderKept(List<String> lines, int threads, int perThread) {
        for (int t = 0; t < threads; t++) {
            String marker = "[Worker" + t + "] msg ";
            int expected = 0;
            for (String line : lines) {
                int at = line.indexOf(marker);
                if (at >= 0) {
                    if (Integer.parseInt(line.substring(at + marker.length())) != expected) {
                        return false;
                    }
                    expected++;
                }
            }
            if (expected != perThread) {
                return false;
            }
        }
        return true;
    }
}

enum Level {
    DEBUG, INFO, WARN, ERROR;

    boolean atLeast(Level other) {
        return compareTo(other) >= 0;
    }
}

record LogRecord(Level level, String loggerName, String message, Instant time) { }

// A clock that moves one second on each call, so the demo prints fixed timestamps.
class TickingClock extends Clock {
    private final AtomicLong seconds = new AtomicLong();
    private final Instant start;

    TickingClock(Instant start) {
        this.start = start;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return start.plusSeconds(seconds.getAndIncrement());
    }
}

interface Formatter {
    String format(LogRecord record);
}

class SimpleFormatter implements Formatter {
    @Override
    public String format(LogRecord r) {
        return r.time() + " " + r.level() + " [" + r.loggerName() + "] " + r.message();
    }
}

class JsonFormatter implements Formatter {
    @Override
    public String format(LogRecord r) {
        return "{\"time\":\"" + r.time() + "\",\"level\":\"" + r.level() + "\",\"logger\":\""
                + escape(r.loggerName()) + "\",\"message\":\"" + escape(r.message()) + "\"}";
    }

    // Escapes the characters that RFC 8259 requires: quote, backslash, and controls below 0x20.
    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}

interface Appender {
    void append(LogRecord record);

    default void close() { }
}

class ConsoleAppender implements Appender {
    private final PrintStream out;
    private final Formatter formatter;

    ConsoleAppender(PrintStream out, Formatter formatter) {
        this.out = out;
        this.formatter = formatter;
    }

    @Override
    public synchronized void append(LogRecord record) {
        out.println(formatter.format(record));
    }
}

// Writes to any Writer. Production code passes a writer that Files.newBufferedWriter opens.
class FileAppender implements Appender {
    private final Writer writer;
    private final Formatter formatter;

    FileAppender(Writer writer, Formatter formatter) {
        this.writer = writer;
        this.formatter = formatter;
    }

    @Override
    public synchronized void append(LogRecord record) {
        try {
            writer.write(formatter.format(record));
            writer.write(System.lineSeparator());
            writer.flush(); // one flush per record: slower, but a crash loses no written line
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write log record", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot close log writer", e);
        }
    }
}

class InMemoryAppender implements Appender {
    private final List<String> lines = new ArrayList<>();
    private final Formatter formatter;

    InMemoryAppender(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public synchronized void append(LogRecord record) {
        lines.add(formatter.format(record));
    }

    synchronized List<String> lines() {
        return List.copyOf(lines);
    }
}

class FailingAppender implements Appender {
    @Override
    public void append(LogRecord record) {
        throw new IllegalStateException("disk full");
    }
}

// Decorator: puts records on a bounded queue. One worker thread writes them to the target.
class AsyncAppender implements Appender {
    private static final LogRecord STOP = new LogRecord(Level.ERROR, "", "", Instant.EPOCH);
    private final BlockingQueue<LogRecord> queue;
    private final Appender target;
    private final Thread worker;
    private final AtomicInteger failures = new AtomicInteger();

    AsyncAppender(Appender target, int capacity) {
        this.target = target;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.worker = new Thread(this::drain, "async-appender");
        this.worker.start();
    }

    @Override
    public void append(LogRecord record) {
        try {
            queue.put(record); // blocks when the queue is full, so no record is lost
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void drain() {
        try {
            while (true) {
                LogRecord record = queue.take();
                if (record == STOP) {
                    return;
                }
                try {
                    target.append(record);
                } catch (RuntimeException e) {
                    failures.incrementAndGet(); // a failing target must not stop the worker
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    int failures() {
        return failures.get();
    }

    @Override
    public void close() {
        try {
            queue.put(STOP);
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        target.close();
    }
}

// Chain of Responsibility: each handler writes records at or above its level, then forwards.
class LogHandler {
    private final Level minimum;
    private final Appender appender;
    private final AtomicInteger failures = new AtomicInteger();
    private LogHandler next;

    LogHandler(Level minimum, Appender appender) {
        this.minimum = minimum;
        this.appender = appender;
    }

    LogHandler setNext(LogHandler next) {
        this.next = next;
        return next;
    }

    void handle(LogRecord record) {
        if (record.level().atLeast(minimum)) {
            try {
                appender.append(record);
            } catch (RuntimeException e) {
                failures.incrementAndGet(); // logging must not break the caller
            }
        }
        if (next != null) {
            next.handle(record);
        }
    }

    int failures() {
        return failures.get();
    }

    void close() {
        appender.close();
        if (next != null) {
            next.close();
        }
    }
}

class Logger {
    private final String name;
    private final LogManager manager;
    private volatile Level level;

    Logger(String name, LogManager manager, Level level) {
        this.name = name;
        this.manager = manager;
        this.level = level;
    }

    void setLevel(Level level) {
        this.level = level;
    }

    boolean isEnabled(Level candidate) {
        return candidate.atLeast(level);
    }

    void debug(String message) {
        log(Level.DEBUG, () -> message);
    }

    void info(String message) {
        log(Level.INFO, () -> message);
    }

    void warn(String message) {
        log(Level.WARN, () -> message);
    }

    void error(String message) {
        log(Level.ERROR, () -> message);
    }

    void log(Level candidate, Supplier<String> message) {
        if (!isEnabled(candidate)) {
            return;
        }
        Instant now = manager.clock().instant();
        manager.dispatch(new LogRecord(candidate, name, message.get(), now));
    }
}

class LogManager {
    private final Clock clock;
    private final Level defaultLevel;
    private final LogHandler head;
    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    LogManager(Clock clock, Level defaultLevel, LogHandler head) {
        this.clock = clock;
        this.defaultLevel = defaultLevel;
        this.head = head;
    }

    Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, n -> new Logger(n, this, defaultLevel));
    }

    Clock clock() {
        return clock;
    }

    void dispatch(LogRecord record) {
        head.handle(record);
    }

    void close() {
        head.close();
    }
}

// Singleton form: simple to call from anywhere, but it is global mutable state.
enum GlobalLogging {
    INSTANCE;

    private final AtomicReference<LogManager> manager = new AtomicReference<>();

    static void install(LogManager logManager) {
        INSTANCE.manager.set(logManager);
    }

    static Logger logger(String name) {
        LogManager current = INSTANCE.manager.get();
        if (current == null) {
            throw new IllegalStateException("Logging is not installed");
        }
        return current.getLogger(name);
    }
}
