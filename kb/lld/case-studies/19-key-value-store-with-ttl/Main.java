import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) throws Exception {
        ManualClock clock = new ManualClock(0);
        Store store = new Store(clock);
        Session session = new Session(store, clock);

        System.out.println("== Get, put, delete ==");
        session.put("name", "ada");
        System.out.println("get name: " + show(session.get("name")));
        System.out.println("get city: " + show(session.get("city")));
        System.out.println("delete name: " + session.delete("name"));
        System.out.println("delete name again: " + session.delete("name"));

        System.out.println("== Lazy expiry ==");
        session.put("token", "t-1", 5_000);
        System.out.println("ttl token: " + session.ttl("token") + " ms");
        clock.advance(3_000);
        System.out.println("after 3000 ms: ttl " + session.ttl("token") + " ms, get "
                + show(session.get("token")));
        clock.advance(2_000);
        System.out.println("after 5000 ms: ttl " + session.ttl("token") + ", get "
                + show(session.get("token")));
        System.out.println("entries held in memory: " + store.rawSize());

        System.out.println("== Active expiry ==");
        session.put("a", "1", 1_000);
        session.put("b", "2", 1_000);
        session.put("c", "3", 2_000);
        session.put("d", "4");
        clock.advance(1_500);
        System.out.println("entries held before sweep: " + store.rawSize());
        System.out.println("sweep removed: " + store.sweep());
        System.out.println("entries held after sweep: " + store.rawSize());
        clock.advance(500);
        System.out.println("sweep removed: " + store.sweep() + ", live keys: "
                + store.snapshot().keys());

        System.out.println("== Overwrite resets the TTL ==");
        session.put("k", "v1", 1_000);
        clock.advance(800);
        session.put("k", "v2");
        clock.advance(5_000);
        System.out.println("sweep removed: " + store.sweep() + ", get k: "
                + show(session.get("k")));

        System.out.println("== Nested transactions ==");
        Session other = new Session(store, clock);
        session.put("x", "1");
        session.begin();
        session.put("x", "2");
        session.put("y", "1");
        System.out.println("inside tx1: x=" + show(session.get("x")) + ", y="
                + show(session.get("y")) + ", depth " + session.depth());
        System.out.println("other session: x=" + show(other.get("x")) + ", y="
                + show(other.get("y")));
        session.begin();
        session.delete("x");
        System.out.println("inside tx2: x=" + show(session.get("x")) + ", depth "
                + session.depth());
        session.rollback();
        System.out.println("after tx2 rollback: x=" + show(session.get("x")));
        session.commit();
        System.out.println("after tx1 commit: other session x=" + show(other.get("x"))
                + ", y=" + show(other.get("y")));
        session.begin();
        session.put("x", "3");
        session.begin();
        session.put("z", "9");
        session.commit();
        System.out.println("tx4 merged into tx3: z=" + show(session.get("z"))
                + ", other session z=" + show(other.get("z")));
        session.rollback();
        System.out.println("after tx3 rollback: x=" + show(session.get("x")) + ", z="
                + show(session.get("z")));
        try {
            session.commit();
        } catch (IllegalStateException e) {
            System.out.println("commit with no transaction: " + e.getMessage());
        }
        session.begin();
        session.put("short", "lived", 100);
        clock.advance(200);
        session.commit();
        System.out.println("key that expired inside the tx: " + show(session.get("short")));

        System.out.println("== Snapshots ==");
        Snapshot before = store.snapshot();
        session.put("x", "changed");
        session.delete("y");
        session.put("w", "new");
        System.out.println("snapshot keys: " + before.keys() + ", x=" + show(before.get("x")));
        System.out.println("live keys now: " + store.snapshot().keys() + ", x="
                + show(session.get("x")));
        store.restore(before);
        System.out.println("after restore: " + store.snapshot().keys() + ", x="
                + show(session.get("x")) + ", y=" + show(session.get("y")));

        System.out.println("== Concurrency ==");
        runConcurrencyDemo(store, clock);
    }

    private static void runConcurrencyDemo(Store store, ManualClock clock) throws Exception {
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);
        Thread[] candidates = new Thread[8];
        for (int i = 0; i < candidates.length; i++) {
            String name = "node-" + i;
            candidates[i] = new Thread(() -> {
                await(go);
                if (store.putIfAbsent("leader", name)) {
                    winners.incrementAndGet();
                }
            });
            candidates[i].start();
        }
        go.countDown();
        for (Thread t : candidates) {
            t.join();
        }
        String leader = store.get("leader").orElseThrow();
        System.out.println("putIfAbsent winners among 8 threads: " + winners.get()
                + ", leader is a candidate: " + leader.startsWith("node-"));

        AtomicInteger torn = new AtomicInteger();
        Thread writer = new Thread(() -> {
            Session w = new Session(store, clock);
            for (int i = 1; i <= 300; i++) {
                w.begin();
                w.put("a", String.valueOf(i));
                w.put("b", String.valueOf(i));
                w.commit();
            }
        });
        Thread[] readers = new Thread[2];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = new Thread(() -> {
                for (int n = 0; n < 300; n++) {
                    Snapshot snap = store.snapshot();
                    if (!snap.get("a").equals(snap.get("b"))) {
                        torn.incrementAndGet();
                    }
                }
            });
        }
        writer.start();
        for (Thread t : readers) {
            t.start();
        }
        writer.join();
        for (Thread t : readers) {
            t.join();
        }
        System.out.println("Torn snapshots (a differs from b): " + torn.get());
        System.out.println("Final a=" + store.get("a").orElseThrow() + ", b="
                + store.get("b").orElseThrow());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String show(Optional<String> value) {
        return value.orElse("<absent>");
    }
}

interface TimeSource {
    long nowMillis();
}

final class ManualClock implements TimeSource {
    private final AtomicLong now;

    ManualClock(long startMillis) {
        this.now = new AtomicLong(startMillis);
    }

    void advance(long millis) {
        now.addAndGet(millis);
    }

    @Override
    public long nowMillis() {
        return now.get();
    }
}

record Entry(String value, long expiresAt) {
    static final long NO_TTL = -1;
    private static final long NEVER = Long.MAX_VALUE;
    static final Entry DELETED = new Entry(null, NEVER);

    static Entry create(String value, long ttlMillis, long now) {
        if (ttlMillis == NO_TTL) {
            return new Entry(value, NEVER);
        }
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("TTL must be positive: " + ttlMillis);
        }
        return new Entry(value, Math.addExact(now, ttlMillis));
    }

    boolean expiredAt(long now) {
        return expiresAt <= now;
    }

    boolean hasTtl() {
        return expiresAt != NEVER;
    }
}

record Snapshot(long takenAt, SortedMap<String, Entry> entries) {
    Snapshot {
        entries = Collections.unmodifiableSortedMap(new TreeMap<>(entries));
    }

    Optional<String> get(String key) {
        Entry entry = entries.get(key);
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    Set<String> keys() {
        return entries.keySet();
    }
}

final class Store {
    private record Expiry(long expiresAt, String key, Entry entry) { }

    private final TimeSource clock;
    private final Map<String, Entry> data = new HashMap<>();
    private final PriorityQueue<Expiry> expiries =
            new PriorityQueue<>(Comparator.comparingLong(Expiry::expiresAt));

    Store(TimeSource clock) {
        this.clock = clock;
    }

    synchronized Optional<Entry> find(String key) {
        return Optional.ofNullable(liveEntry(key));
    }

    synchronized Optional<String> get(String key) {
        return find(key).map(Entry::value);
    }

    synchronized void put(String key, Entry entry) {
        data.put(key, entry);
        if (entry.hasTtl()) {
            expiries.add(new Expiry(entry.expiresAt(), key, entry));
        }
    }

    synchronized boolean putIfAbsent(String key, String value) {
        if (liveEntry(key) != null) {
            return false;
        }
        put(key, Entry.create(value, Entry.NO_TTL, clock.nowMillis()));
        return true;
    }

    synchronized boolean delete(String key) {
        boolean existed = liveEntry(key) != null;
        data.remove(key);
        return existed;
    }

    // Active expiry: remove every entry whose deadline has passed.
    synchronized int sweep() {
        long now = clock.nowMillis();
        int removed = 0;
        while (!expiries.isEmpty() && expiries.peek().expiresAt() <= now) {
            Expiry due = expiries.poll();
            if (data.get(due.key()) == due.entry()) {   // skip stale records
                data.remove(due.key());
                removed++;
            }
        }
        return removed;
    }

    synchronized int rawSize() {
        return data.size();
    }

    // Commit: apply all changes of a transaction in one critical section.
    synchronized void apply(Map<String, Entry> changes) {
        long now = clock.nowMillis();
        for (Map.Entry<String, Entry> change : changes.entrySet()) {
            Entry entry = change.getValue();
            if (entry == Entry.DELETED || entry.expiredAt(now)) {
                data.remove(change.getKey());
            } else {
                put(change.getKey(), entry);
            }
        }
    }

    synchronized Snapshot snapshot() {
        long now = clock.nowMillis();
        SortedMap<String, Entry> copy = new TreeMap<>();
        for (Map.Entry<String, Entry> e : data.entrySet()) {
            if (!e.getValue().expiredAt(now)) {
                copy.put(e.getKey(), e.getValue());
            }
        }
        return new Snapshot(now, copy);
    }

    synchronized void restore(Snapshot snapshot) {
        long now = clock.nowMillis();
        data.clear();
        expiries.clear();
        for (Map.Entry<String, Entry> e : snapshot.entries().entrySet()) {
            if (!e.getValue().expiredAt(now)) {
                put(e.getKey(), e.getValue());
            }
        }
    }

    // Lazy expiry: an expired entry is removed when a caller reads it.
    private Entry liveEntry(String key) {
        Entry entry = data.get(key);
        if (entry != null && entry.expiredAt(clock.nowMillis())) {
            data.remove(key);
            return null;
        }
        return entry;
    }
}

// One client. A session is not thread-safe: each thread uses its own session.
final class Session {
    private final Store store;
    private final TimeSource clock;
    private final Deque<Map<String, Entry>> transactions = new ArrayDeque<>();

    Session(Store store, TimeSource clock) {
        this.store = store;
        this.clock = clock;
    }

    Optional<String> get(String key) {
        Entry entry = resolve(key);
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    void put(String key, String value) {
        put(key, value, Entry.NO_TTL);
    }

    void put(String key, String value, long ttlMillis) {
        Entry entry = Entry.create(value, ttlMillis, clock.nowMillis());
        if (transactions.isEmpty()) {
            store.put(key, entry);
        } else {
            transactions.peek().put(key, entry);
        }
    }

    boolean delete(String key) {
        if (transactions.isEmpty()) {
            return store.delete(key);
        }
        boolean existed = resolve(key) != null;
        transactions.peek().put(key, Entry.DELETED);
        return existed;
    }

    // -2 if the key is absent, -1 if the key has no TTL, else the remaining milliseconds.
    long ttl(String key) {
        Entry entry = resolve(key);
        if (entry == null) {
            return -2;
        }
        return entry.hasTtl() ? entry.expiresAt() - clock.nowMillis() : -1;
    }

    void begin() {
        transactions.push(new HashMap<>());
    }

    void commit() {
        Map<String, Entry> top = popOrThrow();
        if (transactions.isEmpty()) {
            store.apply(top);
        } else {
            transactions.peek().putAll(top);
        }
    }

    void rollback() {
        popOrThrow();
    }

    int depth() {
        return transactions.size();
    }

    private Map<String, Entry> popOrThrow() {
        if (transactions.isEmpty()) {
            throw new IllegalStateException("No open transaction");
        }
        return transactions.pop();
    }

    // Read through the layers: the newest transaction first, then the store.
    private Entry resolve(String key) {
        for (Map<String, Entry> layer : transactions) {
            Entry entry = layer.get(key);
            if (entry != null) {
                boolean gone = entry == Entry.DELETED || entry.expiredAt(clock.nowMillis());
                return gone ? null : entry;
            }
        }
        return store.find(key).orElse(null);
    }
}
