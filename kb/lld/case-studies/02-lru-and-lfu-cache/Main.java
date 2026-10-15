import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("== 1. LRU cache with capacity 3 ==");
        List<String> evicted = new ArrayList<>();
        Cache<String, Integer> lru = new PolicyCache<>(3, new LruPolicy<>(),
                (k, v) -> evicted.add(k + "=" + v));
        lru.put("a", 1);
        lru.put("b", 2);
        lru.put("c", 3);
        System.out.println("get a -> " + lru.get("a").orElse(null));
        lru.put("d", 4);
        System.out.println("get b -> " + lru.get("b").orElse(null));
        lru.put("c", 30);
        lru.put("e", 5);
        System.out.println("evicted " + evicted + ", size " + lru.size());

        System.out.println("== 2. LRU and LFU choose different victims ==");
        for (EvictionPolicy<Integer> policy : List.of(new LruPolicy<Integer>(),
                new LfuPolicy<Integer>())) {
            List<Integer> victims = new ArrayList<>();
            Cache<Integer, String> cache = new PolicyCache<>(2, policy, (k, v) -> victims.add(k));
            cache.put(1, "one");
            cache.put(2, "two");
            cache.get(1);
            cache.get(1);
            cache.put(3, "three");
            cache.put(4, "four");
            System.out.println(policy.getClass().getSimpleName() + " evicted " + victims);
        }

        System.out.println("== 3. LFU breaks a frequency tie by recency ==");
        List<Integer> victims = new ArrayList<>();
        Cache<Integer, String> lfu = new PolicyCache<>(2, new LfuPolicy<>(),
                (k, v) -> victims.add(k));
        lfu.put(1, "one");
        lfu.put(2, "two");
        lfu.get(1);
        lfu.put(3, "three");
        lfu.get(3);
        lfu.put(4, "four");
        System.out.println("evicted " + victims);

        System.out.println("== 4. Hand-written LRU against LinkedHashMap ==");
        Cache<Integer, Integer> mine = new PolicyCache<>(50, new LruPolicy<>());
        Cache<Integer, Integer> jdk = new LinkedHashLruCache<>(50);
        Random random = new Random(42);
        int mineHits = 0;
        int jdkHits = 0;
        boolean sameAnswers = true;
        for (int i = 0; i < 5000; i++) {
            int key = random.nextInt(80);
            if (random.nextInt(3) == 0) {
                mine.put(key, i);
                jdk.put(key, i);
            } else {
                Optional<Integer> a = mine.get(key);
                Optional<Integer> b = jdk.get(key);
                sameAnswers &= a.equals(b);
                mineHits += a.isPresent() ? 1 : 0;
                jdkHits += b.isPresent() ? 1 : 0;
            }
        }
        System.out.println("hits " + mineHits + " and " + jdkHits + ", same answers: "
                + sameAnswers);

        System.out.println("== 5. TTL decorator ==");
        MutableClock clock = new MutableClock(Instant.parse("2026-01-05T08:00:00Z"));
        Cache<String, String> ttl = new TtlCache<>(new PolicyCache<>(2, new LruPolicy<>()),
                Duration.ofSeconds(10), clock);
        ttl.put("session", "alice");
        clock.advance(Duration.ofSeconds(5));
        System.out.println("after 5s: " + ttl.get("session").orElse("expired"));
        clock.advance(Duration.ofSeconds(6));
        System.out.println("after 11s: " + ttl.get("session").orElse("expired"));

        System.out.println("== 6. Remove keeps the LFU buckets correct ==");
        Cache<String, Integer> removable = new PolicyCache<>(3, new LfuPolicy<>());
        removable.put("x", 1);
        removable.put("y", 2);
        removable.get("y");
        removable.remove("x");
        removable.put("z", 3);
        removable.put("w", 4);
        removable.put("v", 5);
        System.out.println("x=" + removable.get("x").orElse(null) + " y="
                + removable.get("y").orElse(null) + " size=" + removable.size());

        System.out.println("== 7. Thread-safe cache under 8 writers ==");
        AtomicInteger evictions = new AtomicInteger();
        Cache<Integer, Integer> shared = new ThreadSafeCache<>(
                new PolicyCache<>(100, new LruPolicy<>(), (k, v) -> evictions.incrementAndGet()));
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            final int base = t * 1000;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    shared.put(base + i, i);
                    shared.get(base + i / 2);
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("size " + shared.size() + ", evictions " + evictions.get());

        System.out.println("== 8. Capacity 1 and the update of an existing key ==");
        for (EvictionPolicy<String> policy : List.of(new LruPolicy<String>(),
                new LfuPolicy<String>())) {
            List<String> gone = new ArrayList<>();
            Cache<String, Integer> one = new PolicyCache<>(1, policy, (k, v) -> gone.add(k));
            one.put("a", 1);
            one.put("a", 2);
            one.put("b", 3);
            System.out.println(policy.getClass().getSimpleName() + ": a="
                    + one.get("a").orElse(null) + " b=" + one.get("b").orElse(null)
                    + " evicted " + gone + " size " + one.size());
        }
    }
}

interface Cache<K, V> {
    Optional<V> get(K key);

    void put(K key, V value);

    void remove(K key);

    int size();
}

/** Tracks the order of keys and picks the victim. It stores no values. */
interface EvictionPolicy<K> {
    void keyAdded(K key);

    void keyAccessed(K key);

    void keyRemoved(K key);

    /** Removes the victim from the policy state and returns it. */
    K evictKey();
}

/** A cache entry in a doubly linked list. The list has two sentinel nodes. */
final class Node<K> {
    final K key;
    int frequency = 1;
    Node<K> prev;
    Node<K> next;

    Node(K key) {
        this.key = key;
    }
}

final class NodeList<K> {
    private final Node<K> head = new Node<>(null);
    private final Node<K> tail = new Node<>(null);

    NodeList() {
        head.next = tail;
        tail.prev = head;
    }

    boolean isEmpty() {
        return head.next == tail;
    }

    void addFirst(Node<K> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    void remove(Node<K> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    Node<K> last() {
        return isEmpty() ? null : tail.prev;
    }
}

final class LruPolicy<K> implements EvictionPolicy<K> {
    private final Map<K, Node<K>> nodes = new HashMap<>();
    private final NodeList<K> order = new NodeList<>();

    @Override
    public void keyAdded(K key) {
        Node<K> node = new Node<>(key);
        nodes.put(key, node);
        order.addFirst(node);
    }

    @Override
    public void keyAccessed(K key) {
        Node<K> node = nodes.get(key);
        order.remove(node);
        order.addFirst(node);
    }

    @Override
    public void keyRemoved(K key) {
        order.remove(nodes.remove(key));
    }

    @Override
    public K evictKey() {
        Node<K> victim = order.last();
        order.remove(victim);
        nodes.remove(victim.key);
        return victim.key;
    }
}

final class LfuPolicy<K> implements EvictionPolicy<K> {
    private final Map<K, Node<K>> nodes = new HashMap<>();
    private final Map<Integer, NodeList<K>> buckets = new HashMap<>();
    private int minFrequency;

    @Override
    public void keyAdded(K key) {
        Node<K> node = new Node<>(key);
        nodes.put(key, node);
        bucket(1).addFirst(node);
        minFrequency = 1;
    }

    @Override
    public void keyAccessed(K key) {
        Node<K> node = nodes.get(key);
        detach(node);
        if (!buckets.containsKey(minFrequency)) {
            minFrequency++;
        }
        node.frequency++;
        bucket(node.frequency).addFirst(node);
    }

    @Override
    public void keyRemoved(K key) {
        detach(nodes.remove(key));
        if (!buckets.containsKey(minFrequency) && !buckets.isEmpty()) {
            minFrequency = lowestFrequency();
        }
    }

    @Override
    public K evictKey() {
        if (!buckets.containsKey(minFrequency)) {
            // Only after an eviction with no keyAdded after it, as in a weighted cache.
            minFrequency = lowestFrequency();
        }
        Node<K> victim = buckets.get(minFrequency).last();
        detach(victim);
        nodes.remove(victim.key);
        return victim.key;
    }

    /** O(F) for F distinct frequencies. Throws NoSuchElementException if no key exists. */
    private int lowestFrequency() {
        return buckets.keySet().stream().min(Integer::compare).orElseThrow();
    }

    private NodeList<K> bucket(int frequency) {
        return buckets.computeIfAbsent(frequency, f -> new NodeList<>());
    }

    private void detach(Node<K> node) {
        NodeList<K> bucket = buckets.get(node.frequency);
        bucket.remove(node);
        if (bucket.isEmpty()) {
            buckets.remove(node.frequency);
        }
    }
}

/** Stores the values. The policy decides which key leaves when the cache is full. */
final class PolicyCache<K, V> implements Cache<K, V> {
    private final int capacity;
    private final Map<K, V> values = new HashMap<>();
    private final EvictionPolicy<K> policy;
    private final BiConsumer<K, V> evictionListener;

    PolicyCache(int capacity, EvictionPolicy<K> policy) {
        this(capacity, policy, (k, v) -> { });
    }

    PolicyCache(int capacity, EvictionPolicy<K> policy, BiConsumer<K, V> evictionListener) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.capacity = capacity;
        this.policy = policy;
        this.evictionListener = evictionListener;
    }

    @Override
    public Optional<V> get(K key) {
        V value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        policy.keyAccessed(key);
        return Optional.of(value);
    }

    @Override
    public void put(K key, V value) {
        Objects.requireNonNull(value, "value");
        if (values.containsKey(key)) {
            values.put(key, value);
            policy.keyAccessed(key);
            return;
        }
        if (values.size() == capacity) {
            K victim = policy.evictKey();
            V evictedValue = values.remove(victim);
            evictionListener.accept(victim, evictedValue);
        }
        values.put(key, value);
        policy.keyAdded(key);
    }

    @Override
    public void remove(K key) {
        if (values.remove(key) != null) {
            policy.keyRemoved(key);
        }
    }

    @Override
    public int size() {
        return values.size();
    }
}

/** The JDK version: LinkedHashMap in access order removes the eldest entry. */
final class LinkedHashLruCache<K, V> implements Cache<K, V> {
    private final Map<K, V> map;

    LinkedHashLruCache(int capacity) {
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public void put(K key, V value) {
        map.put(key, Objects.requireNonNull(value, "value"));
    }

    @Override
    public void remove(K key) {
        map.remove(key);
    }

    @Override
    public int size() {
        return map.size();
    }
}

/** One lock for the whole cache. A read also changes the order, so a read needs the lock. */
final class ThreadSafeCache<K, V> implements Cache<K, V> {
    private final Cache<K, V> delegate;
    private final ReentrantLock lock = new ReentrantLock();

    ThreadSafeCache(Cache<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<V> get(K key) {
        lock.lock();
        try {
            return delegate.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            delegate.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(K key) {
        lock.lock();
        try {
            delegate.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return delegate.size();
        } finally {
            lock.unlock();
        }
    }
}

/** Adds a time to live to any cache. Expired entries leave when a read finds them. */
final class TtlCache<K, V> implements Cache<K, V> {
    /** The stored form of a value. Package-private, so callers can build the delegate. */
    record Expiring<V>(V value, Instant expiresAt) { }

    private final Cache<K, Expiring<V>> delegate;
    private final Duration ttl;
    private final Clock clock;

    TtlCache(Cache<K, Expiring<V>> delegate, Duration ttl, Clock clock) {
        this.delegate = delegate;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public Optional<V> get(K key) {
        Optional<Expiring<V>> entry = delegate.get(key);
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(entry.get().expiresAt())) {
            delegate.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.get().value());
    }

    @Override
    public void put(K key, V value) {
        delegate.put(key, new Expiring<>(value, clock.instant().plus(ttl)));
    }

    @Override
    public void remove(K key) {
        delegate.remove(key);
    }

    @Override
    public int size() {
        return delegate.size();
    }
}

/** A clock that the demo moves by hand, so the results are the same on every run. */
final class MutableClock extends Clock {
    private final AtomicReference<Instant> now;

    MutableClock(Instant start) {
        this.now = new AtomicReference<>(start);
    }

    void advance(Duration duration) {
        now.updateAndGet(current -> current.plus(duration));
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
        return now.get();
    }
}
