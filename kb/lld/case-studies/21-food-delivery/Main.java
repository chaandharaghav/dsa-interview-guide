import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static final Instant START = Instant.parse("2026-01-15T12:00:00Z");

    public static void main(String[] args) throws Exception {
        happyPath();
        rejectAndCancel();
        invalidRequests();
        assignmentStrategies();
        concurrentDispatch();
    }

    static OrderService newService(ManualClock clock, PartnerAssignmentStrategy strategy,
                                   List<DeliveryPartner> partners) {
        Restaurant pizza = new Restaurant("r1", "Pizza Place", new Point(0, 0), 15);
        pizza.addItem(new MenuItem("margherita", "Margherita", 1200));
        pizza.addItem(new MenuItem("cola", "Cola", 250));
        pizza.addItem(new MenuItem("tiramisu", "Tiramisu", 600));
        Restaurant sushi = new Restaurant("r2", "Sushi Corner", new Point(5, 5), 20);
        sushi.addItem(new MenuItem("salmon-roll", "Salmon Roll", 900));
        OrderService service = new OrderService(clock, new DispatchService(partners, strategy));
        service.addRestaurant(pizza);
        service.addRestaurant(sushi);
        return service;
    }

    static List<DeliveryPartner> twoPartners() {
        return List.of(
                new DeliveryPartner("p1", "Asha", new Point(1, 1), 45),
                new DeliveryPartner("p2", "Ben", new Point(4, 4), 49));
    }

    static void happyPath() {
        System.out.println("== 1. Order lifecycle with status notifications ==");
        ManualClock clock = new ManualClock(START);
        OrderService service = newService(clock, new NearestPartnerStrategy(), twoPartners());
        service.addListener(new ConsoleNotifier(clock));
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("margherita", 2);
        items.put("cola", 1);
        Order order = service.place("alice", "r1", new Point(3, 2), items);
        System.out.println("Subtotal " + money(order.subtotalCents())
                + ", delivery fee " + money(order.deliveryFeeCents())
                + ", total " + money(order.totalCents()));
        System.out.println("Promised delivery by " + clockText(order.promisedAt()));
        clock.advance(Duration.ofMinutes(2));
        service.accept(order.id());
        clock.advance(Duration.ofMinutes(1));
        service.startPreparing(order.id());
        clock.advance(Duration.ofMinutes(12));
        service.markReady(order.id());
        clock.advance(Duration.ofMinutes(1));
        service.pickUp(order.id());
        clock.advance(Duration.ofMinutes(14));
        service.deliver(order.id());
        System.out.println("Final status: " + order.status() + " at "
                + clockText(order.deliveredAt()) + ", on time: " + order.deliveredOnTime());
    }

    static void rejectAndCancel() {
        System.out.println("== 2. Reject and cancel ==");
        ManualClock clock = new ManualClock(START);
        OrderService service = newService(clock, new NearestPartnerStrategy(), twoPartners());
        service.addListener(new ConsoleNotifier(clock));
        Order rejected = service.place("bob", "r1", new Point(2, 2), Map.of("cola", 1));
        service.reject(rejected.id());
        Order early = service.place("bob", "r1", new Point(2, 2), Map.of("margherita", 1));
        service.accept(early.id());
        long refundEarly = service.cancel(early.id());
        System.out.println("Refund before cooking: " + money(refundEarly));
        Order late = service.place("bob", "r1", new Point(2, 2), Map.of("margherita", 1));
        service.accept(late.id());
        service.startPreparing(late.id());
        long refundLate = service.cancel(late.id());
        System.out.println("Refund while cooking: " + money(refundLate));
        Order ready = service.place("bob", "r1", new Point(2, 2), Map.of("margherita", 1));
        service.accept(ready.id());
        service.startPreparing(ready.id());
        service.markReady(ready.id());
        service.pickUp(ready.id());
        tryAndPrint("Cancel after pickup", () -> service.cancel(ready.id()));
        System.out.println("Free partners after cancels: " + service.dispatch().freePartnerCount());
    }

    static void invalidRequests() {
        System.out.println("== 3. Invalid requests ==");
        ManualClock clock = new ManualClock(START);
        OrderService service = newService(clock, new NearestPartnerStrategy(), twoPartners());
        Point home = new Point(1, 0);
        tryAndPrint("Unknown restaurant", () -> service.place("c", "r9", home, Map.of("x", 1)));
        tryAndPrint("Unknown item", () -> service.place("c", "r1", home, Map.of("pasta", 1)));
        tryAndPrint("Zero quantity", () -> service.place("c", "r1", home, Map.of("cola", 0)));
        tryAndPrint("Empty order", () -> service.place("c", "r1", home, Map.of()));
        service.restaurant("r1").markSoldOut("tiramisu");
        tryAndPrint("Sold out item", () -> service.place("c", "r1", home, Map.of("tiramisu", 1)));
        service.restaurant("r1").setOpen(false);
        tryAndPrint("Closed restaurant", () -> service.place("c", "r1", home, Map.of("cola", 1)));
        service.restaurant("r1").setOpen(true);
        Order order = service.place("c", "r1", home, Map.of("cola", 1));
        tryAndPrint("Skip a step", () -> service.markReady(order.id()));
        Order second = service.place("c", "r1", home, Map.of("cola", 1));
        Order third = service.place("c", "r1", home, Map.of("cola", 1));
        Order fourth = service.place("c", "r1", home, Map.of("cola", 1));
        service.accept(order.id());
        service.accept(second.id());
        service.accept(third.id());
        service.accept(fourth.id());
        System.out.println("Orders waiting for a partner: " + service.dispatch().waitingCount());
        service.cancel(fourth.id());
        System.out.println("Orders waiting after a waiting order is cancelled: "
                + service.dispatch().waitingCount());
        service.startPreparing(third.id());
        service.markReady(third.id());
        tryAndPrint("Pick up without a partner", () -> service.pickUp(third.id()));
        service.cancel(second.id());
        System.out.println("After " + second.id() + " is cancelled, " + third.id()
                + " gets partner " + third.partner().name() + ", waiting orders: "
                + service.dispatch().waitingCount());
        tryAndPrint("Pick up with a partner", () -> service.pickUp(third.id()));
    }

    static void assignmentStrategies() {
        System.out.println("== 4. Partner assignment strategies ==");
        List<DeliveryPartner> partners = List.of(
                new DeliveryPartner("p1", "Asha", new Point(1, 1), 45),
                new DeliveryPartner("p2", "Ben", new Point(4, 4), 49),
                new DeliveryPartner("p3", "Chen", new Point(6, 5), 40));
        PartnerAssignmentStrategy[] strategies = {
                new NearestPartnerStrategy(), new HighestRatedStrategy()};
        for (PartnerAssignmentStrategy strategy : strategies) {
            ManualClock clock = new ManualClock(START);
            OrderService service = newService(clock, strategy, partners);
            Order order = service.place("dina", "r2", new Point(7, 7), Map.of("salmon-roll", 1));
            service.accept(order.id());
            System.out.println(strategy.getClass().getSimpleName() + " picks "
                    + order.partner().name());
        }
    }

    static void concurrentDispatch() throws Exception {
        System.out.println("== 5. Concurrent partner assignment ==");
        ManualClock clock = new ManualClock(START);
        List<DeliveryPartner> partners = List.of(
                new DeliveryPartner("p1", "Asha", new Point(1, 1), 45),
                new DeliveryPartner("p2", "Ben", new Point(4, 4), 49),
                new DeliveryPartner("p3", "Chen", new Point(6, 5), 47));
        OrderService service = newService(clock, new NearestPartnerStrategy(), partners);
        EventCounter counter = new EventCounter();
        service.addListener(counter);
        int orderCount = 8;
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            orders.add(service.place("c" + i, "r1", new Point(2, 2), Map.of("cola", 1)));
        }
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (Order order : orders) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    service.accept(order.id());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        long assigned = orders.stream().filter(o -> o.partner() != null).count();
        long distinct = orders.stream().filter(o -> o.partner() != null)
                .map(o -> o.partner().id()).distinct().count();
        System.out.println("Orders with a partner: " + assigned);
        System.out.println("Distinct partners used: " + distinct);
        System.out.println("Orders waiting for a partner: " + service.dispatch().waitingCount());
        int delivered = 0;
        while (delivered < orderCount) {
            List<Order> ready = new ArrayList<>(orders.stream()
                    .filter(o -> o.status() == OrderStatus.ACCEPTED && o.partner() != null)
                    .sorted(Comparator.comparing(Order::id)).toList());
            for (Order order : ready) {
                service.startPreparing(order.id());
                service.markReady(order.id());
                service.pickUp(order.id());
                service.deliver(order.id());
                delivered++;
            }
        }
        System.out.println("Delivered orders: " + delivered);
        System.out.println("Peak busy partners: " + service.dispatch().peakBusy());
        System.out.println("Double bookings detected: " + service.dispatch().doubleBookings());
        System.out.println("Status events seen: " + counter.statusEvents());
        System.out.println("Partner assignment events seen: " + counter.assignmentEvents());
    }

    static void tryAndPrint(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": accepted");
        } catch (OrderException e) {
            System.out.println(label + " rejected: " + e.getMessage());
        }
    }

    static String money(long cents) {
        return String.format(Locale.ROOT, "$%d.%02d", cents / 100, cents % 100);
    }

    static String clockText(Instant instant) {
        return LocalTime.ofInstant(instant, ZoneOffset.UTC).toString();
    }
}

record Point(int x, int y) {
    int distanceTo(Point other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}

record MenuItem(String id, String name, long priceCents) { }

record OrderLine(MenuItem item, int quantity) {
    long totalCents() {
        return item.priceCents() * quantity;
    }
}

record DeliveryPartner(String id, String name, Point location, int ratingTenths) { }

class OrderException extends RuntimeException {
    OrderException(String message) {
        super(message);
    }
}

/** A clock that only moves when the caller advances it, so the demo is deterministic. */
class ManualClock extends Clock {
    private volatile Instant now;

    ManualClock(Instant start) {
        this.now = start;
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}

enum OrderStatus {
    PLACED(100), ACCEPTED(100), PREPARING(50), READY(0),
    PICKED_UP(0), DELIVERED(0), CANCELLED(0), REJECTED(0);

    private final int refundPercent;

    OrderStatus(int refundPercent) {
        this.refundPercent = refundPercent;
    }

    boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PLACED -> next == ACCEPTED || next == REJECTED || next == CANCELLED;
            case ACCEPTED -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY || next == CANCELLED;
            case READY -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == DELIVERED;
            case DELIVERED, CANCELLED, REJECTED -> false;
        };
    }

    boolean isCancellable() {
        return canMoveTo(CANCELLED);
    }

    long refundFor(long totalCents) {
        return totalCents * refundPercent / 100;
    }
}

class Restaurant {
    private final String id;
    private final String name;
    private final Point location;
    private final int prepMinutes;
    private final Map<String, MenuItem> menu = new LinkedHashMap<>();
    private final Set<String> soldOut = ConcurrentHashMap.newKeySet();
    private volatile boolean open = true;

    Restaurant(String id, String name, Point location, int prepMinutes) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.prepMinutes = prepMinutes;
    }

    void addItem(MenuItem item) {
        menu.put(item.id(), item);
    }

    void markSoldOut(String itemId) {
        soldOut.add(itemId);
    }

    void setOpen(boolean open) {
        this.open = open;
    }

    MenuItem orderableItem(String itemId) {
        MenuItem item = menu.get(itemId);
        if (item == null) {
            throw new OrderException("Item " + itemId + " is not on the menu of " + name);
        }
        if (soldOut.contains(itemId)) {
            throw new OrderException(item.name() + " is sold out");
        }
        return item;
    }

    boolean isOpen() {
        return open;
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    Point location() {
        return location;
    }

    int prepMinutes() {
        return prepMinutes;
    }
}

interface OrderListener {
    void onStatusChange(Order order, OrderStatus from, OrderStatus to);

    default void onPartnerAssigned(Order order, DeliveryPartner partner) { }
}

class Order {
    private final String id;
    private final String customerId;
    private final Restaurant restaurant;
    private final Point deliverTo;
    private final List<OrderLine> lines;
    private final long subtotalCents;
    private final long deliveryFeeCents;
    private final Instant promisedAt;
    private OrderStatus status = OrderStatus.PLACED;
    private DeliveryPartner partner;
    private Instant deliveredAt;

    Order(String id, String customerId, Restaurant restaurant, Point deliverTo,
          List<OrderLine> lines, long deliveryFeeCents, Instant promisedAt) {
        this.id = id;
        this.customerId = customerId;
        this.restaurant = restaurant;
        this.deliverTo = deliverTo;
        this.lines = List.copyOf(lines);
        this.subtotalCents = lines.stream().mapToLong(OrderLine::totalCents).sum();
        this.deliveryFeeCents = deliveryFeeCents;
        this.promisedAt = promisedAt;
    }

    /** Moves the order to the next status and returns the old status. */
    synchronized OrderStatus moveTo(OrderStatus next, Instant now) {
        if (!status.canMoveTo(next)) {
            throw new OrderException(
                    "Order " + id + " cannot move from " + status + " to " + next);
        }
        if (next == OrderStatus.PICKED_UP && partner == null) {
            throw new OrderException("Order " + id + " has no delivery partner yet");
        }
        OrderStatus old = status;
        status = next;
        if (next == OrderStatus.DELIVERED) {
            deliveredAt = now;
        }
        return old;
    }

    /** Sets the partner, unless the order was cancelled. Returns whether it set the partner. */
    synchronized boolean assignPartner(DeliveryPartner partner) {
        if (status == OrderStatus.CANCELLED) {
            return false;
        }
        this.partner = partner;
        return true;
    }

    synchronized OrderStatus status() {
        return status;
    }

    synchronized DeliveryPartner partner() {
        return partner;
    }

    synchronized Instant deliveredAt() {
        return deliveredAt;
    }

    synchronized boolean deliveredOnTime() {
        return deliveredAt != null && !deliveredAt.isAfter(promisedAt);
    }

    long totalCents() {
        return subtotalCents + deliveryFeeCents;
    }

    String id() {
        return id;
    }

    String customerId() {
        return customerId;
    }

    Restaurant restaurant() {
        return restaurant;
    }

    Point deliverTo() {
        return deliverTo;
    }

    List<OrderLine> lines() {
        return lines;
    }

    long subtotalCents() {
        return subtotalCents;
    }

    long deliveryFeeCents() {
        return deliveryFeeCents;
    }

    Instant promisedAt() {
        return promisedAt;
    }
}

interface PartnerAssignmentStrategy {
    Optional<DeliveryPartner> choose(Point pickup, List<DeliveryPartner> available);
}

class NearestPartnerStrategy implements PartnerAssignmentStrategy {
    @Override
    public Optional<DeliveryPartner> choose(Point pickup, List<DeliveryPartner> available) {
        return available.stream().min(Comparator
                .comparingInt((DeliveryPartner p) -> p.location().distanceTo(pickup))
                .thenComparing(DeliveryPartner::id));
    }
}

class HighestRatedStrategy implements PartnerAssignmentStrategy {
    @Override
    public Optional<DeliveryPartner> choose(Point pickup, List<DeliveryPartner> available) {
        return available.stream().min(Comparator
                .comparingInt((DeliveryPartner p) -> -p.ratingTenths())
                .thenComparingInt(p -> p.location().distanceTo(pickup))
                .thenComparing(DeliveryPartner::id));
    }
}

record Assignment(Order order, DeliveryPartner partner) { }

class DispatchService {
    private final List<DeliveryPartner> partners;
    private final PartnerAssignmentStrategy strategy;
    private final Set<String> busyIds = new HashSet<>();
    private final Deque<Order> waiting = new ArrayDeque<>();
    private int peakBusy;
    private int doubleBookings;

    DispatchService(List<DeliveryPartner> partners, PartnerAssignmentStrategy strategy) {
        this.partners = List.copyOf(partners);
        this.strategy = strategy;
    }

    /** Assigns a free partner to the order, or queues the order if no partner is free. */
    synchronized List<Assignment> request(Order order) {
        if (order.status() == OrderStatus.CANCELLED) {
            return List.of();
        }
        waiting.addLast(order);
        return drainWaiting();
    }

    /** Frees the partner of the order, or removes the order from the queue. */
    synchronized List<Assignment> release(Order order) {
        DeliveryPartner partner = order.partner();
        if (partner != null) {
            busyIds.remove(partner.id());
        } else {
            waiting.remove(order);
        }
        return drainWaiting();
    }

    private List<Assignment> drainWaiting() {
        List<Assignment> made = new ArrayList<>();
        while (!waiting.isEmpty()) {
            Order next = waiting.peekFirst();
            List<DeliveryPartner> free = partners.stream()
                    .filter(p -> !busyIds.contains(p.id())).toList();
            Optional<DeliveryPartner> chosen = strategy.choose(next.restaurant().location(), free);
            if (chosen.isEmpty()) {
                break;
            }
            waiting.removeFirst();
            if (!next.assignPartner(chosen.get())) {
                continue;
            }
            if (!busyIds.add(chosen.get().id())) {
                doubleBookings++;
            }
            peakBusy = Math.max(peakBusy, busyIds.size());
            made.add(new Assignment(next, chosen.get()));
        }
        return made;
    }

    synchronized int freePartnerCount() {
        return partners.size() - busyIds.size();
    }

    synchronized int waitingCount() {
        return waiting.size();
    }

    synchronized int peakBusy() {
        return peakBusy;
    }

    synchronized int doubleBookings() {
        return doubleBookings;
    }
}

class OrderService {
    private static final long BASE_FEE_CENTS = 200;
    private static final long FEE_PER_DISTANCE_CENTS = 50;
    private static final int MINUTES_PER_DISTANCE = 3;

    private final Clock clock;
    private final DispatchService dispatch;
    private final Map<String, Restaurant> restaurants = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final List<OrderListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger();

    OrderService(Clock clock, DispatchService dispatch) {
        this.clock = clock;
        this.dispatch = dispatch;
    }

    void addRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.id(), restaurant);
    }

    void addListener(OrderListener listener) {
        listeners.add(listener);
    }

    Restaurant restaurant(String id) {
        return restaurants.get(id);
    }

    DispatchService dispatch() {
        return dispatch;
    }

    Order place(String customerId, String restaurantId, Point deliverTo,
                Map<String, Integer> quantities) {
        Restaurant restaurant = restaurants.get(restaurantId);
        if (restaurant == null) {
            throw new OrderException("Unknown restaurant " + restaurantId);
        }
        if (!restaurant.isOpen()) {
            throw new OrderException(restaurant.name() + " is closed");
        }
        if (quantities.isEmpty()) {
            throw new OrderException("An order needs at least one item");
        }
        List<OrderLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new OrderException("Quantity must be positive for " + entry.getKey());
            }
            lines.add(new OrderLine(restaurant.orderableItem(entry.getKey()), entry.getValue()));
        }
        int distance = restaurant.location().distanceTo(deliverTo);
        long fee = BASE_FEE_CENTS + FEE_PER_DISTANCE_CENTS * distance;
        Duration eta = Duration.ofMinutes(
                restaurant.prepMinutes() + (long) MINUTES_PER_DISTANCE * distance);
        String id = "O-" + sequence.incrementAndGet();
        Order order = new Order(id, customerId, restaurant, deliverTo, lines, fee,
                clock.instant().plus(eta));
        orders.put(id, order);
        notifyStatus(order, null, OrderStatus.PLACED);
        return order;
    }

    void accept(String orderId) {
        Order order = find(orderId);
        transition(order, OrderStatus.ACCEPTED);
        publish(dispatch.request(order));
    }

    void reject(String orderId) {
        transition(find(orderId), OrderStatus.REJECTED);
    }

    void startPreparing(String orderId) {
        transition(find(orderId), OrderStatus.PREPARING);
    }

    void markReady(String orderId) {
        transition(find(orderId), OrderStatus.READY);
    }

    void pickUp(String orderId) {
        transition(find(orderId), OrderStatus.PICKED_UP);
    }

    void deliver(String orderId) {
        Order order = find(orderId);
        transition(order, OrderStatus.DELIVERED);
        publish(dispatch.release(order));
    }

    /** Cancels the order and returns the refund in cents. */
    long cancel(String orderId) {
        Order order = find(orderId);
        if (!order.status().isCancellable()) {
            throw new OrderException(
                    "Order " + orderId + " cannot be cancelled in status " + order.status());
        }
        OrderStatus before = transition(order, OrderStatus.CANCELLED);
        publish(dispatch.release(order));
        return before.refundFor(order.totalCents());
    }

    private OrderStatus transition(Order order, OrderStatus next) {
        OrderStatus from = order.moveTo(next, clock.instant());
        notifyStatus(order, from, next);
        return from;
    }

    private void notifyStatus(Order order, OrderStatus from, OrderStatus to) {
        for (OrderListener listener : listeners) {
            listener.onStatusChange(order, from, to);
        }
    }

    private void publish(List<Assignment> assignments) {
        for (Assignment a : assignments) {
            for (OrderListener listener : listeners) {
                listener.onPartnerAssigned(a.order(), a.partner());
            }
        }
    }

    private Order find(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new OrderException("Unknown order " + orderId);
        }
        return order;
    }
}

class ConsoleNotifier implements OrderListener {
    private final Clock clock;

    ConsoleNotifier(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void onStatusChange(Order order, OrderStatus from, OrderStatus to) {
        System.out.println("[" + Main.clockText(clock.instant()) + "] " + order.id() + " "
                + (from == null ? "created" : from + " -> " + to) + " (customer "
                + order.customerId() + ")");
    }

    @Override
    public void onPartnerAssigned(Order order, DeliveryPartner partner) {
        System.out.println("[" + Main.clockText(clock.instant()) + "] " + order.id()
                + " partner " + partner.name());
    }
}

class EventCounter implements OrderListener {
    private final AtomicInteger statusEvents = new AtomicInteger();
    private final AtomicInteger assignmentEvents = new AtomicInteger();

    @Override
    public void onStatusChange(Order order, OrderStatus from, OrderStatus to) {
        statusEvents.incrementAndGet();
    }

    @Override
    public void onPartnerAssigned(Order order, DeliveryPartner partner) {
        assignmentEvents.incrementAndGet();
    }

    int statusEvents() {
        return statusEvents.get();
    }

    int assignmentEvents() {
        return assignmentEvents.get();
    }
}
