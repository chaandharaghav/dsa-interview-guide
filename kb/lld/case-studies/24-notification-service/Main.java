import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {
        ManualClock clock = new ManualClock(Instant.parse("2026-01-01T00:00:00Z"));
        Map<Channel, InMemorySender> senders = new EnumMap<>(Channel.class);
        for (Channel channel : Channel.values()) {
            senders.put(channel, new InMemorySender());
        }
        UserDirectory users = new UserDirectory();
        users.add(new User("alice", "alice@example.com", "+15550001", "tok-alice"));
        users.add(new User("bob", "bob@example.com", "+15550002", null));
        users.add(new User("carol", "carol@example.com", "+15550003", "tok-carol"));
        TemplateRegistry templates = new TemplateRegistry();
        templates.add(new Template("order-shipped", Map.of(
                Channel.EMAIL, "Hello {name}, your order {order} has shipped.",
                Channel.SMS, "{name}: order {order} shipped.",
                Channel.PUSH, "Order {order} is on its way")));
        templates.add(new Template("sale", Map.of(
                Channel.EMAIL, "Hi {name}, the sale starts today.",
                Channel.PUSH, "Sale starts today")));
        templates.add(new Template("login-code", Map.of(
                Channel.SMS, "Your code is {code}")));
        NotificationService service = new NotificationService(clock, users, templates,
                new ChannelSenders(senders),
                new FixedWindowLimiter(clock, 2, Duration.ofMinutes(1)),
                new RetryPolicy(3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30)));
        service.addListener(event -> System.out.println("  [t+" + clock.elapsedSeconds() + "s] "
                + event));
        service.preferences().set("alice", Category.TRANSACTIONAL, EnumSet.of(Channel.EMAIL,
                Channel.PUSH));
        service.preferences().set("alice", Category.PROMOTIONAL, EnumSet.of(Channel.EMAIL));
        service.preferences().set("bob", Category.TRANSACTIONAL, EnumSet.of(Channel.EMAIL,
                Channel.SMS, Channel.PUSH));

        System.out.println("== 1. Send by user preference ==");
        print(service.send(new NotificationRequest("r1", "alice", Category.TRANSACTIONAL,
                "order-shipped", Map.of("name", "Alice", "order", "A-17"))));
        print(service.send(new NotificationRequest("r2", "bob", Category.TRANSACTIONAL,
                "order-shipped", Map.of("name", "Bob", "order", "B-4"))));
        service.processDue();
        for (Channel channel : Channel.values()) {
            for (String line : senders.get(channel).sent()) {
                System.out.println(channel + " -> " + line);
            }
        }

        System.out.println("== 2. Opt-out and missing address ==");
        print(service.send(new NotificationRequest("r3", "carol", Category.PROMOTIONAL, "sale",
                Map.of("name", "Carol"))));
        service.preferences().set("carol", Category.PROMOTIONAL, EnumSet.of(Channel.PUSH));
        service.preferences().set("bob", Category.PROMOTIONAL, EnumSet.of(Channel.PUSH));
        print(service.send(new NotificationRequest("r4", "bob", Category.PROMOTIONAL, "sale",
                Map.of("name", "Bob"))));

        System.out.println("== 3. Duplicate request id ==");
        print(service.send(new NotificationRequest("r1", "alice", Category.TRANSACTIONAL,
                "order-shipped", Map.of("name", "Alice", "order", "A-17"))));

        System.out.println("== 4. Retry with backoff ==");
        senders.get(Channel.EMAIL).failTransiently("carol@example.com", 2);
        service.preferences().set("carol", Category.TRANSACTIONAL, EnumSet.of(Channel.EMAIL));
        print(service.send(new NotificationRequest("r5", "carol", Category.TRANSACTIONAL,
                "order-shipped", Map.of("name", "Carol", "order", "C-9"))));
        runUntilIdle(service, clock);

        System.out.println("== 5. Retries run out, and a permanent failure ==");
        senders.get(Channel.EMAIL).failTransiently("bob@example.com", 10);
        senders.get(Channel.SMS).rejectPermanently("+15550002");
        print(service.send(new NotificationRequest("r6", "bob", Category.TRANSACTIONAL,
                "order-shipped", Map.of("name", "Bob", "order", "B-5"))));
        runUntilIdle(service, clock);
        System.out.println("dead letters: " + service.deadLetters().size());

        System.out.println("== 6. Rate limit per user ==");
        for (int i = 1; i <= 3; i++) {
            print(service.send(new NotificationRequest("p" + i, "alice", Category.PROMOTIONAL,
                    "sale", Map.of("name", "Alice"))));
        }
        for (int i = 1; i <= 3; i++) {
            print(service.send(new NotificationRequest("s" + i, "alice", Category.SECURITY,
                    "login-code", Map.of("code", "48" + i))));
        }
        runUntilIdle(service, clock);
        clock.advance(Duration.ofSeconds(61));
        print(service.send(new NotificationRequest("p4", "alice", Category.PROMOTIONAL, "sale",
                Map.of("name", "Alice"))));
        runUntilIdle(service, clock);

        System.out.println("== 7. Sixteen threads send to one user ==");
        users.add(new User("dave", "dave@example.com", "+15550004", "tok-dave"));
        service.preferences().set("dave", Category.PROMOTIONAL, EnumSet.of(Channel.EMAIL));
        AtomicInteger queued = new AtomicInteger();
        AtomicInteger limited = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            final int n = i;
            Thread t = new Thread(() -> {
                SendReceipt receipt = service.send(new NotificationRequest("c" + n, "dave",
                        Category.PROMOTIONAL, "sale", Map.of("name", "Dave")));
                if (receipt.decisions().get(Channel.EMAIL) == Decision.QUEUED) {
                    queued.incrementAndGet();
                } else {
                    limited.incrementAndGet();
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("queued=" + queued + " rate-limited=" + limited);
    }

    static void print(SendReceipt receipt) {
        System.out.println(receipt);
    }

    /** Runs the delivery loop and moves the manual clock to the next retry time. */
    static void runUntilIdle(NotificationService service, ManualClock clock) {
        service.processDue();
        while (service.nextDueAt() != null) {
            clock.advanceTo(service.nextDueAt());
            service.processDue();
        }
    }
}

/** A clock that only moves when the demo calls advance. */
final class ManualClock extends Clock {
    private final Instant start;
    private final AtomicLong offsetMillis = new AtomicLong();

    ManualClock(Instant start) {
        this.start = start;
    }

    void advance(Duration duration) {
        offsetMillis.addAndGet(duration.toMillis());
    }

    void advanceTo(Instant target) {
        offsetMillis.set(Duration.between(start, target).toMillis());
    }

    long elapsedSeconds() {
        return offsetMillis.get() / 1000;
    }

    @Override
    public Instant instant() {
        return start.plusMillis(offsetMillis.get());
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}

enum Channel { EMAIL, SMS, PUSH }

enum Category {
    TRANSACTIONAL(false), PROMOTIONAL(false), SECURITY(true);

    private final boolean bypassesRateLimit;

    Category(boolean bypassesRateLimit) {
        this.bypassesRateLimit = bypassesRateLimit;
    }

    boolean bypassesRateLimit() {
        return bypassesRateLimit;
    }
}

record User(String id, String email, String phone, String deviceToken) {
    String addressFor(Channel channel) {
        return switch (channel) {
            case EMAIL -> email;
            case SMS -> phone;
            case PUSH -> deviceToken;
        };
    }
}

final class UserDirectory {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    void add(User user) {
        users.put(user.id(), user);
    }

    User find(String id) {
        return users.get(id);
    }
}

/** What the user allows. A category with no entry allows nothing for PROMOTIONAL. */
final class PreferenceStore {
    private final Map<String, Map<Category, Set<Channel>>> byUser = new ConcurrentHashMap<>();

    void set(String userId, Category category, Set<Channel> channels) {
        byUser.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(category, Set.copyOf(channels));
    }

    /** Opt-in for promotions. Opt-out for the other categories. The result is read-only. */
    Set<Channel> allowed(String userId, Category category) {
        Set<Channel> chosen = byUser.getOrDefault(userId, Map.of()).get(category);
        if (chosen != null) {
            return chosen;
        }
        return category == Category.PROMOTIONAL ? Set.of() : Set.of(Channel.values());
    }
}

record Template(String id, Map<Channel, String> bodies) {
    Template {
        bodies = Map.copyOf(bodies);
    }
}

final class TemplateRegistry {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    void add(Template template) {
        templates.put(template.id(), template);
    }

    /** Returns the body for the channel, or null if the template has no body for it. */
    String render(String templateId, Channel channel, Map<String, String> params) {
        Template template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("unknown template " + templateId);
        }
        String body = template.bodies().get(channel);
        if (body == null) {
            return null;
        }
        // One pass over the body: a value that contains "{x}" is not expanded again.
        return PLACEHOLDER.matcher(body).replaceAll(match -> {
            String value = params.get(match.group(1));
            if (value == null) {
                throw new IllegalArgumentException("missing parameter " + match.group(1)
                        + " in template " + templateId);
            }
            return Matcher.quoteReplacement(value);
        });
    }
}

record NotificationRequest(String requestId, String userId, Category category,
                           String templateId, Map<String, String> params) {
}

enum Decision { QUEUED, OPTED_OUT, NO_TEMPLATE, NO_ADDRESS, RATE_LIMITED, DUPLICATE }

record SendReceipt(String requestId, Map<Channel, Decision> decisions) {
}

/** One message for one channel. The record is immutable. A retry makes a new record. */
record Delivery(long seq, String requestId, String userId, Channel channel, String address,
                String body, int attempt, Instant dueAt) {
    Delivery retryAt(Instant next) {
        return new Delivery(seq, requestId, userId, channel, address, body, attempt + 1, next);
    }
}

enum Outcome { SENT, TRANSIENT_FAILURE, PERMANENT_FAILURE }

/** Strategy: one implementation for each channel. */
interface ChannelSender {
    Outcome send(Delivery delivery);
}

final class ChannelSenders {
    private final Map<Channel, ? extends ChannelSender> senders;

    ChannelSenders(Map<Channel, ? extends ChannelSender> senders) {
        this.senders = senders;
    }

    ChannelSender forChannel(Channel channel) {
        return senders.get(channel);
    }
}

/** A sender that records what it sends and fails on command. It does not call a provider. */
final class InMemorySender implements ChannelSender {
    private final List<String> sent = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> transientFailuresLeft = new ConcurrentHashMap<>();
    private final Set<String> rejected = ConcurrentHashMap.newKeySet();

    void failTransiently(String address, int times) {
        transientFailuresLeft.put(address, times);
    }

    void rejectPermanently(String address) {
        rejected.add(address);
    }

    List<String> sent() {
        return sent;
    }

    @Override
    public Outcome send(Delivery delivery) {
        if (rejected.contains(delivery.address())) {
            return Outcome.PERMANENT_FAILURE;
        }
        // computeIfPresent is atomic, so two workers never use the same failure twice.
        Integer left = transientFailuresLeft.computeIfPresent(delivery.address(), (a, n) -> n - 1);
        if (left != null && left >= 0) {
            return Outcome.TRANSIENT_FAILURE;
        }
        sent.add(delivery.address() + ": " + delivery.body());
        return Outcome.SENT;
    }
}

/** Exponential backoff without jitter, so the demo is deterministic. */
record RetryPolicy(int maxAttempts, Duration baseDelay, double multiplier, Duration maxDelay) {
    boolean canRetry(int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    Duration delayAfter(int attemptsMade) {
        double millis = baseDelay.toMillis() * Math.pow(multiplier, attemptsMade - 1);
        return Duration.ofMillis((long) Math.min(millis, maxDelay.toMillis()));
    }
}

interface RateLimiter {
    boolean tryAcquire(String key);
}

/** At most `limit` permits for each key in each window. The window starts at the first permit. */
final class FixedWindowLimiter implements RateLimiter {
    private record Window(Instant start, int used) {
    }

    private final Clock clock;
    private final int limit;
    private final Duration length;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    FixedWindowLimiter(Clock clock, int limit, Duration length) {
        this.clock = clock;
        this.limit = limit;
        this.length = length;
    }

    @Override
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        boolean[] granted = new boolean[1];
        windows.compute(key, (k, w) -> {
            if (w == null || !now.isBefore(w.start().plus(length))) {
                granted[0] = true;
                return new Window(now, 1);
            }
            if (w.used() < limit) {
                granted[0] = true;
                return new Window(w.start(), w.used() + 1);
            }
            return w;
        });
        return granted[0];
    }
}

/** Observer interface. The service sends one event for each step of a delivery. */
interface DeliveryListener {
    void onEvent(DeliveryEvent event);
}

record DeliveryEvent(String type, Delivery delivery, String detail) {
    @Override
    public String toString() {
        return type + " " + delivery.channel() + " " + delivery.userId() + " attempt "
                + delivery.attempt() + detail;
    }
}

/** The facade. It decides, renders, queues, sends, and retries. */
final class NotificationService {
    private final Clock clock;
    private final UserDirectory users;
    private final TemplateRegistry templates;
    private final ChannelSenders senders;
    private final RateLimiter limiter;
    private final RetryPolicy retry;
    private final PreferenceStore preferences = new PreferenceStore();
    private final Set<String> seenRequests = ConcurrentHashMap.newKeySet();
    private final AtomicLong seq = new AtomicLong();
    private final PriorityQueue<Delivery> queue = new PriorityQueue<>(
            Comparator.comparing(Delivery::dueAt).thenComparingLong(Delivery::seq));
    private final List<Delivery> deadLetters = new CopyOnWriteArrayList<>();
    private final List<DeliveryListener> listeners = new CopyOnWriteArrayList<>();

    NotificationService(Clock clock, UserDirectory users, TemplateRegistry templates,
                        ChannelSenders senders, RateLimiter limiter, RetryPolicy retry) {
        this.clock = clock;
        this.users = users;
        this.templates = templates;
        this.senders = senders;
        this.limiter = limiter;
        this.retry = retry;
    }

    PreferenceStore preferences() {
        return preferences;
    }

    void addListener(DeliveryListener listener) {
        listeners.add(listener);
    }

    List<Delivery> deadLetters() {
        return deadLetters;
    }

    SendReceipt send(NotificationRequest request) {
        // Phase 1 has no side effects. A bad request throws here and does not use its id.
        User user = users.find(request.userId());
        if (user == null) {
            throw new IllegalArgumentException("unknown user " + request.userId());
        }
        Set<Channel> allowed = preferences.allowed(request.userId(), request.category());
        Map<Channel, String> bodies = new EnumMap<>(Channel.class);
        for (Channel channel : allowed) {
            String body = templates.render(request.templateId(), channel, request.params());
            if (body != null) {
                bodies.put(channel, body);
            }
        }
        // Phase 2 claims the request id. Phase 3 takes permits and queues.
        Map<Channel, Decision> decisions = new EnumMap<>(Channel.class);
        if (!seenRequests.add(request.requestId())) {
            for (Channel channel : Channel.values()) {
                decisions.put(channel, Decision.DUPLICATE);
            }
            return new SendReceipt(request.requestId(), decisions);
        }
        for (Channel channel : Channel.values()) {
            String body = bodies.get(channel);
            String address = user.addressFor(channel);
            if (!allowed.contains(channel)) {
                decisions.put(channel, Decision.OPTED_OUT);
            } else if (body == null) {
                decisions.put(channel, Decision.NO_TEMPLATE);
            } else if (address == null) {
                decisions.put(channel, Decision.NO_ADDRESS);
            } else if (!request.category().bypassesRateLimit()
                    && !limiter.tryAcquire(limitKey(user, request, channel))) {
                decisions.put(channel, Decision.RATE_LIMITED);
            } else {
                enqueue(new Delivery(seq.incrementAndGet(), request.requestId(), user.id(),
                        channel, address, body, 1, clock.instant()));
                decisions.put(channel, Decision.QUEUED);
            }
        }
        return new SendReceipt(request.requestId(), decisions);
    }

    private static String limitKey(User user, NotificationRequest request, Channel channel) {
        return user.id() + "/" + request.category() + "/" + channel;
    }

    private void enqueue(Delivery delivery) {
        synchronized (queue) {
            queue.add(delivery);
        }
    }

    /** The due time of the next delivery, or null if the queue is empty. */
    Instant nextDueAt() {
        synchronized (queue) {
            return queue.isEmpty() ? null : queue.peek().dueAt();
        }
    }

    private Delivery pollDue() {
        synchronized (queue) {
            Delivery head = queue.peek();
            if (head == null || head.dueAt().isAfter(clock.instant())) {
                return null;
            }
            return queue.poll();
        }
    }

    /** One worker pass: send every delivery that is due now. */
    void processDue() {
        Delivery delivery;
        while ((delivery = pollDue()) != null) {
            Outcome outcome = attempt(delivery);
            switch (outcome) {
                case SENT -> publish("SENT", delivery, "");
                case PERMANENT_FAILURE -> deadLetter(delivery, " (permanent failure)");
                case TRANSIENT_FAILURE -> {
                    if (retry.canRetry(delivery.attempt())) {
                        Duration wait = retry.delayAfter(delivery.attempt());
                        enqueue(delivery.retryAt(clock.instant().plus(wait)));
                        publish("RETRY", delivery, " in " + wait.toSeconds() + "s");
                    } else {
                        deadLetter(delivery, " (retries exhausted)");
                    }
                }
            }
        }
    }

    /** A sender that throws has a bug or met an unknown error. The delivery gets a retry. */
    private Outcome attempt(Delivery delivery) {
        try {
            return senders.forChannel(delivery.channel()).send(delivery);
        } catch (RuntimeException e) {
            publish("ERROR", delivery, " " + e.getClass().getSimpleName());
            return Outcome.TRANSIENT_FAILURE;
        }
    }

    private void deadLetter(Delivery delivery, String reason) {
        deadLetters.add(delivery);
        publish("DEAD", delivery, reason);
    }

    private void publish(String type, Delivery delivery, String detail) {
        DeliveryEvent event = new DeliveryEvent(type, delivery, detail);
        for (DeliveryListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
