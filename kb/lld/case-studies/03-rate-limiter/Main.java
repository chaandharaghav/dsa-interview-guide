import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        demoTokenBucket();
        demoBoundaryBurst();
        demoWeightedEstimate();
        demoPerClientLimits();
        demoConcurrency();
        demoValidation();
    }

    private static void demoTokenBucket() {
        System.out.println("== Token bucket: capacity 5, refill 1 token per second ==");
        ManualTimeSource time = new ManualTimeSource();
        RateLimiterService service =
                new RateLimiterService(() -> new TokenBucketStrategy(5, 1.0, time));
        System.out.println("t=0ms, burst of 6 requests:");
        for (int i = 1; i <= 6; i++) {
            System.out.println("  request " + i + " -> " + service.check("alice"));
        }
        time.advance(2000);
        System.out.println("t=2000ms, 3 requests after 2 seconds of refill:");
        for (int i = 1; i <= 3; i++) {
            System.out.println("  request " + i + " -> " + service.check("alice"));
        }
    }

    private static void demoBoundaryBurst() {
        System.out.println("== Boundary burst: limit 3 per 1000 ms, requests at 900, 1000 ms ==");
        runBoundaryScenario("fixed window", time -> new FixedWindowStrategy(3, 1000, time));
        runBoundaryScenario("sliding log", time -> new SlidingWindowLogStrategy(3, 1000, time));
        runBoundaryScenario("sliding counter",
                time -> new SlidingWindowCounterStrategy(3, 1000, time));
    }

    private static void runBoundaryScenario(
            String name, Function<TimeSource, RateLimitStrategy> factory) {
        ManualTimeSource time = new ManualTimeSource();
        RateLimitStrategy limiter = factory.apply(time);
        time.advance(900);
        int allowedBefore = countAllowed(limiter, 3);
        time.advance(100);
        int allowedAfter = countAllowed(limiter, 3);
        System.out.println(name + ": allowed " + allowedBefore + " at t=900, "
                + allowedAfter + " at t=1000, total " + (allowedBefore + allowedAfter)
                + " in 100 ms");
    }

    private static void demoWeightedEstimate() {
        System.out.println("== Sliding counter: weight of the previous window ==");
        ManualTimeSource time = new ManualTimeSource();
        RateLimitStrategy limiter = new SlidingWindowCounterStrategy(3, 1000, time);
        time.advance(900);
        System.out.println("t=900: allowed " + countAllowed(limiter, 3) + " of 3");
        time.advance(600);
        System.out.println("t=1500: previous window weighs 0.5, estimate 1.5, allowed "
                + countAllowed(limiter, 3) + " of 3");
        Decision denied = limiter.tryAcquire();
        System.out.println("t=1500: next request -> " + denied);
        time.advance(denied.retryAfterMillis());
        System.out.println(
                "t=" + time.nowMillis() + ": after the hint -> " + limiter.tryAcquire());
        time.advance(2000 - time.nowMillis());
        System.out.println("t=2000: allowed " + countAllowed(limiter, 3) + " of 3");
    }

    private static int countAllowed(RateLimitStrategy limiter, int requests) {
        int allowed = 0;
        for (int i = 0; i < requests; i++) {
            if (limiter.tryAcquire().allowed()) {
                allowed++;
            }
        }
        return allowed;
    }

    private static void demoPerClientLimits() {
        System.out.println("== Per-client limits: default 2 per window, premium client 4 ==");
        ManualTimeSource time = new ManualTimeSource();
        RateLimiterService service =
                new RateLimiterService(() -> new FixedWindowStrategy(2, 1000, time));
        service.configure("premium", () -> new FixedWindowStrategy(4, 1000, time));
        for (String client : List.of("alice", "bob", "premium")) {
            int allowed = 0;
            for (int i = 0; i < 6; i++) {
                if (service.check(client).allowed()) {
                    allowed++;
                }
            }
            System.out.println(client + ": " + allowed + " of 6 allowed");
        }
        time.advance(1000);
        System.out.println("after the window ends, alice -> " + service.check("alice"));
    }

    private static void demoConcurrency() throws InterruptedException {
        System.out.println("== Concurrency: 8 threads x 1000 requests, one client, limit 100 ==");
        ManualTimeSource time = new ManualTimeSource();
        RateLimiterService service =
                new RateLimiterService(() -> new TokenBucketStrategy(100, 0.0, time));
        AtomicInteger allowed = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    if (service.check("shared-client").allowed()) {
                        allowed.incrementAndGet();
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("allowed in total: " + allowed.get() + " of 8000");
    }

    private static void demoValidation() {
        System.out.println("== Validation ==");
        try {
            new TokenBucketStrategy(0, 1.0, new ManualTimeSource());
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }
        try {
            new FixedWindowStrategy(5, 0, new ManualTimeSource());
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }
    }
}

// Contract: nowMillis() never decreases. The strategies rely on it.
interface TimeSource {
    long nowMillis();
}

// System.nanoTime() is not related to the wall clock, so a clock change cannot move it back.
// Its origin is arbitrary and the value can be negative, so the strategies use floorMod.
final class SystemTimeSource implements TimeSource {
    @Override
    public long nowMillis() {
        return Math.floorDiv(System.nanoTime(), 1_000_000L);
    }
}

final class ManualTimeSource implements TimeSource {
    private long now;

    @Override
    public synchronized long nowMillis() {
        return now;
    }

    synchronized void advance(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("time must not move backward");
        }
        now += millis;
    }
}

record Decision(boolean allowed, long retryAfterMillis) {
    static Decision allow() {
        return new Decision(true, 0);
    }

    static Decision deny(long retryAfterMillis) {
        return new Decision(false, retryAfterMillis);
    }

    @Override
    public String toString() {
        return allowed ? "ALLOWED" : "DENIED (retry after " + retryAfterMillis + " ms)";
    }
}

interface RateLimitStrategy {
    Decision tryAcquire();
}

final class TokenBucketStrategy implements RateLimitStrategy {
    private final double capacity;
    private final double refillPerMilli;
    private final TimeSource time;
    private double tokens;
    private long lastRefill;

    TokenBucketStrategy(int capacity, double refillPerSecond, TimeSource time) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (!Double.isFinite(refillPerSecond) || refillPerSecond < 0) {
            throw new IllegalArgumentException("refill rate must be finite and not negative");
        }
        this.capacity = capacity;
        this.refillPerMilli = refillPerSecond / 1000.0;
        this.time = time;
        this.tokens = capacity;
        this.lastRefill = time.nowMillis();
    }

    @Override
    public synchronized Decision tryAcquire() {
        long now = time.nowMillis();
        tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerMilli);
        lastRefill = now;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return Decision.allow();
        }
        if (refillPerMilli == 0) {
            return Decision.deny(Long.MAX_VALUE);
        }
        return Decision.deny((long) Math.ceil((1.0 - tokens) / refillPerMilli));
    }
}

final class FixedWindowStrategy implements RateLimitStrategy {
    private final int limit;
    private final long windowMillis;
    private final TimeSource time;
    private long windowStart;
    private int count;

    FixedWindowStrategy(int limit, long windowMillis, TimeSource time) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.time = time;
        this.windowStart = alignedStart(time.nowMillis());
    }

    private long alignedStart(long now) {
        return now - Math.floorMod(now, windowMillis);
    }

    @Override
    public synchronized Decision tryAcquire() {
        long now = time.nowMillis();
        long start = alignedStart(now);
        if (start != windowStart) {
            windowStart = start;
            count = 0;
        }
        if (count < limit) {
            count++;
            return Decision.allow();
        }
        return Decision.deny(windowStart + windowMillis - now);
    }
}

final class SlidingWindowLogStrategy implements RateLimitStrategy {
    private final int limit;
    private final long windowMillis;
    private final TimeSource time;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    SlidingWindowLogStrategy(int limit, long windowMillis, TimeSource time) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.time = time;
    }

    @Override
    public synchronized Decision tryAcquire() {
        long now = time.nowMillis();
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowMillis) {
            timestamps.removeFirst();
        }
        if (timestamps.size() < limit) {
            timestamps.addLast(now);
            return Decision.allow();
        }
        return Decision.deny(timestamps.peekFirst() + windowMillis - now);
    }
}

final class SlidingWindowCounterStrategy implements RateLimitStrategy {
    private final int limit;
    private final long windowMillis;
    private final TimeSource time;
    private long currentStart;
    private int currentCount;
    private int previousCount;

    SlidingWindowCounterStrategy(int limit, long windowMillis, TimeSource time) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (windowMillis > Long.MAX_VALUE / (2L * limit + 1)) {
            throw new IllegalArgumentException("limit x window overflows a long");
        }
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.time = time;
        long now = time.nowMillis();
        this.currentStart = now - Math.floorMod(now, windowMillis);
    }

    @Override
    public synchronized Decision tryAcquire() {
        long now = time.nowMillis();
        long start = now - Math.floorMod(now, windowMillis);
        if (start != currentStart) {
            previousCount = (start - currentStart == windowMillis) ? currentCount : 0;
            currentCount = 0;
            currentStart = start;
        }
        long elapsed = now - currentStart;
        // The estimate times windowMillis, in exact long arithmetic:
        // previous * (window - elapsed) / window + current.
        long scaledEstimate =
                previousCount * (windowMillis - elapsed) + currentCount * windowMillis;
        if (scaledEstimate + windowMillis <= limit * windowMillis) {
            currentCount++;
            return Decision.allow();
        }
        return Decision.deny(retryAfter(elapsed));
    }

    // The wait until the estimate plus one request fits in the limit.
    private long retryAfter(long elapsed) {
        long room = limit - 1L - currentCount;
        if (room >= 0) {
            // The weight of the previous window must drop. Here previousCount > 0,
            // because with previousCount == 0 the request is allowed.
            long allowedAt = windowMillis - room * windowMillis / previousCount;
            return allowedAt - elapsed;
        }
        // currentCount == limit. In the next window, currentCount becomes the previous count.
        long allowedAt = windowMillis - (limit - 1L) * windowMillis / currentCount;
        return windowMillis - elapsed + allowedAt;
    }
}

final class RateLimiterService {
    private final Supplier<RateLimitStrategy> defaultPolicy;
    private final Map<String, Supplier<RateLimitStrategy>> overrides = new ConcurrentHashMap<>();
    private final Map<String, RateLimitStrategy> limiters = new ConcurrentHashMap<>();

    RateLimiterService(Supplier<RateLimitStrategy> defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    void configure(String clientId, Supplier<RateLimitStrategy> policy) {
        overrides.put(clientId, policy);
        limiters.remove(clientId);
    }

    Decision check(String clientId) {
        RateLimitStrategy limiter = limiters.computeIfAbsent(
                clientId, id -> overrides.getOrDefault(id, defaultPolicy).get());
        return limiter.tryAcquire();
    }
}
