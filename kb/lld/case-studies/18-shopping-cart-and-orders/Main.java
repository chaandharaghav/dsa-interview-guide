import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-02T10:00:00Z"));
        FakeGateway gateway = new FakeGateway();
        ShopService shop = demoShop(clock, gateway);

        System.out.println("== 1. Cart and price ==");
        shop.addToCart("alice", "SKU-LAPTOP", 1);
        shop.addToCart("alice", "SKU-PEN", 3);
        shop.addToCart("alice", "SKU-NOTEBOOK", 2);
        shop.setQuantity("alice", "SKU-NOTEBOOK", 1);
        shop.addToCart("alice", "SKU-MUG", 1);
        shop.removeFromCart("alice", "SKU-MUG");
        System.out.println("cart: " + shop.cartText("alice"));
        System.out.println("no coupon: " + shop.quote("alice"));
        shop.applyCoupon("alice", "SAVE10");
        System.out.println("SAVE10: " + shop.quote("alice"));

        System.out.println("== 2. Coupon rules ==");
        shop.addToCart("bob", "SKU-PEN", 1);
        attempt("unknown code", () -> shop.applyCoupon("bob", "NOPE"));
        attempt("expired code", () -> shop.applyCoupon("bob", "FLAT5"));
        attempt("subtotal too low", () -> shop.applyCoupon("bob", "SAVE10"));
        attempt("stationery code", () -> shop.applyCoupon("bob", "STATIONERY"));
        System.out.println("bob: " + shop.quote("bob"));

        System.out.println("== 3. Checkout and idempotent retry ==");
        System.out.println("laptops before: " + shop.inventory().available("SKU-LAPTOP"));
        Order first = shop.checkout("alice", "key-1", "tok_ok");
        System.out.println(first.describe());
        System.out.println("laptops after: " + shop.inventory().available("SKU-LAPTOP")
                + ", reserved " + shop.inventory().reserved("SKU-LAPTOP"));
        Order replay = shop.checkout("alice", "key-1", "tok_ok");
        System.out.println("same key returns the same order: " + (replay == first)
                + ", charges=" + gateway.successfulCharges());
        attempt("checkout with an empty cart", () -> shop.checkout("alice", "key-2", "tok_ok"));

        System.out.println("== 4. Declined payment ==");
        shop.addToCart("carol", "SKU-HEADPHONES", 2);
        Order declined = shop.checkout("carol", "key-3", "tok_decline");
        System.out.println(declined.describe());
        System.out.println("headphones: " + shop.inventory().available("SKU-HEADPHONES")
                + " available, " + shop.inventory().reserved("SKU-HEADPHONES") + " reserved");
        Order retry = shop.checkout("carol", "key-4", "tok_ok");
        System.out.println(retry.describe());

        System.out.println("== 5. Order lifecycle ==");
        shop.ship(first.id());
        System.out.println(first.describe());
        attempt("cancel a shipped order", () -> shop.cancel(first.id()));
        shop.deliver(first.id());
        attempt("ship a delivered order", () -> shop.ship(first.id()));
        shop.cancel(retry.id());
        System.out.println(retry.describe());
        System.out.println("headphones: " + shop.inventory().available("SKU-HEADPHONES")
                + " available, refunds=" + gateway.refunds());
        System.out.println("history " + first.id() + ": " + first.history());
        System.out.println("history " + retry.id() + ": " + retry.history());

        System.out.println("== 6. Hold expires during a slow payment ==");
        shop.addToCart("dave", "SKU-HEADPHONES", 1);
        gateway.beforeCharge(() -> {
            clock.advance(Duration.ofMinutes(20));
            shop.inventory().expire();
        });
        Order slow = shop.checkout("dave", "key-5", "tok_ok");
        gateway.beforeCharge(() -> { });
        System.out.println(slow.describe());
        System.out.println("headphones: " + shop.inventory().available("SKU-HEADPHONES")
                + " available, refunds=" + gateway.refunds());

        System.out.println("== 7. Ten buyers, three mugs ==");
        FakeGateway gateway2 = new FakeGateway();
        ShopService rush = demoShop(clock, gateway2);
        List<String> buyers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            buyers.add("buyer" + i);
            rush.addToCart("buyer" + i, "SKU-MUG", 1);
        }
        AtomicInteger paid = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        runTogether(buyers, buyer -> {
            try {
                Order order = rush.checkout(buyer, "key-" + buyer, "tok_ok");
                if (order.status() == OrderStatus.PAID) {
                    paid.incrementAndGet();
                }
            } catch (OutOfStockException e) {
                soldOut.incrementAndGet();
            }
        });
        System.out.println("paid=" + paid + " soldOut=" + soldOut + " mugsLeft="
                + rush.inventory().available("SKU-MUG") + " reserved="
                + rush.inventory().reserved("SKU-MUG") + " charges="
                + gateway2.successfulCharges());

        System.out.println("== 8. Five clicks on the pay button ==");
        rush.addToCart("erin", "SKU-NOTEBOOK", 2);
        Set<Order> orders = ConcurrentHashMap.newKeySet();
        int chargesBefore = gateway2.successfulCharges();
        runTogether(List.of("1", "2", "3", "4", "5"),
                n -> orders.add(rush.checkout("erin", "key-erin", "tok_ok")));
        System.out.println("distinctOrders=" + orders.size() + " newCharges="
                + (gateway2.successfulCharges() - chargesBefore) + " notebooksLeft="
                + rush.inventory().available("SKU-NOTEBOOK"));

        System.out.println("== 9. A coupon with two uses, five buyers ==");
        List<String> shoppers = List.of("s0", "s1", "s2", "s3", "s4");
        for (String shopper : shoppers) {
            rush.addToCart(shopper, "SKU-PEN", 4);
            rush.applyCoupon(shopper, "LIMITED");
        }
        AtomicInteger redeemed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        runTogether(shoppers, shopper -> {
            try {
                rush.checkout(shopper, "key-" + shopper, "tok_ok");
                redeemed.incrementAndGet();
            } catch (CouponException e) {
                rejected.incrementAndGet();
            }
        });
        System.out.println("redeemed=" + redeemed + " rejected=" + rejected + " pensLeft="
                + rush.inventory().available("SKU-PEN") + " reserved="
                + rush.inventory().reserved("SKU-PEN"));

        System.out.println("== 10. The gateway charges, then the response is lost ==");
        shop.addToCart("frank", "SKU-PEN", 1);
        int chargesBeforeTimeout = gateway.successfulCharges();
        gateway.loseNextResponse();
        attempt("first try", () -> shop.checkout("frank", "key-6", "tok_ok"));
        Order recovered = shop.checkout("frank", "key-6", "tok_ok");
        System.out.println("retry with the same key: " + recovered.describe() + ", new charges="
                + (gateway.successfulCharges() - chargesBeforeTimeout));
    }

    static ShopService demoShop(Clock clock, PaymentGateway gateway) {
        Inventory inventory = new Inventory(clock, Duration.ofMinutes(15));
        PricingService pricing = new PricingService(List.of(new BuyXGetOneFree("SKU-PEN", 2)));
        ShopService shop = new ShopService(inventory, pricing, gateway, clock);
        shop.addProduct(new Product("SKU-LAPTOP", "Laptop", "electronics", 49_900), 5);
        shop.addProduct(new Product("SKU-HEADPHONES", "Headphones", "electronics", 7_900), 10);
        shop.addProduct(new Product("SKU-PEN", "Pen", "stationery", 150), 50);
        shop.addProduct(new Product("SKU-NOTEBOOK", "Notebook", "stationery", 450), 20);
        shop.addProduct(new Product("SKU-MUG", "Mug", "home", 1_200), 3);
        Instant now = clock.instant();
        shop.addCoupon(new Coupon("SAVE10", new PercentageDiscount(10, 2_000), 5_000,
                now.plus(Duration.ofDays(60)), Integer.MAX_VALUE));
        shop.addCoupon(new Coupon("FLAT5", new FlatDiscount(500), 1_000,
                now.minus(Duration.ofDays(1)), Integer.MAX_VALUE));
        shop.addCoupon(new Coupon("STATIONERY", new CategoryDiscount("stationery", 15), 0,
                now.plus(Duration.ofDays(30)), Integer.MAX_VALUE));
        shop.addCoupon(new Coupon("LIMITED", new FlatDiscount(200), 0,
                now.plus(Duration.ofDays(30)), 2));
        return shop;
    }

    private static <T> void runTogether(List<T> items, Consumer<T> action)
            throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (T item : items) {
            threads.add(new Thread(() -> {
                try {
                    go.await();
                    action.accept(item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        threads.forEach(Thread::start);
        go.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": done");
        } catch (ShopException | IllegalStateException | IllegalArgumentException e) {
            System.out.println(label + ": refused (" + e.getMessage() + ")");
        }
    }

    static String money(long cents) {
        return String.format("%d.%02d", cents / 100, cents % 100);
    }
}

record Product(String sku, String name, String category, long priceCents) {
}

record OrderLine(String sku, String name, String category, long unitCents, int quantity) {
    long totalCents() {
        return unitCents * quantity;
    }
}

record PriceBreakdown(long subtotalCents, long discountCents, long totalCents) {
    @Override
    public String toString() {
        return "subtotal " + Main.money(subtotalCents) + ", discount "
                + Main.money(discountCents) + ", total " + Main.money(totalCents);
    }
}

class ShopException extends RuntimeException {
    ShopException(String message) {
        super(message);
    }
}

final class OutOfStockException extends ShopException {
    OutOfStockException(String sku, int wanted, int available) {
        super(sku + ": wanted " + wanted + ", available " + available);
    }
}

final class CouponException extends ShopException {
    CouponException(String message) {
        super(message);
    }
}

final class PaymentTimeoutException extends ShopException {
    PaymentTimeoutException(String idempotencyKey) {
        super("no answer from the gateway for key " + idempotencyKey);
    }
}

final class ReservationExpiredException extends ShopException {
    ReservationExpiredException(String reservationId) {
        super("reservation " + reservationId + " is gone or expired");
    }
}

record Reservation(String id, Map<String, Integer> items, Instant expiresAt) {
}

final class Inventory {
    private final Map<String, Integer> available = new HashMap<>();
    private final Map<String, Integer> reserved = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final Clock clock;
    private final Duration holdTime;
    private final AtomicLong seq = new AtomicLong();

    Inventory(Clock clock, Duration holdTime) {
        this.clock = clock;
        this.holdTime = holdTime;
    }

    synchronized void restock(String sku, int quantity) {
        available.merge(sku, quantity, Integer::sum);
    }

    synchronized int available(String sku) {
        return available.getOrDefault(sku, 0);
    }

    synchronized int reserved(String sku) {
        return reserved.getOrDefault(sku, 0);
    }

    /** All or nothing: either every item is held, or nothing changes. */
    synchronized Reservation reserve(Map<String, Integer> items) {
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            int free = available(item.getKey());
            if (free < item.getValue()) {
                throw new OutOfStockException(item.getKey(), item.getValue(), free);
            }
        }
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            available.merge(item.getKey(), -item.getValue(), Integer::sum);
            reserved.merge(item.getKey(), item.getValue(), Integer::sum);
        }
        Reservation reservation = new Reservation("R-" + seq.incrementAndGet(),
                Map.copyOf(items), clock.instant().plus(holdTime));
        reservations.put(reservation.id(), reservation);
        return reservation;
    }

    /** The sale is final. The held units leave the stock. */
    synchronized void commit(String reservationId) {
        Reservation reservation = reservations.remove(reservationId);
        if (reservation == null) {
            throw new ReservationExpiredException(reservationId);
        }
        if (!reservation.expiresAt().isAfter(clock.instant())) {
            // The hold timed out, and the sweeper has not run yet.
            giveBack(reservation);
            throw new ReservationExpiredException(reservationId);
        }
        reservation.items().forEach((sku, qty) -> reserved.merge(sku, -qty, Integer::sum));
    }

    /** The sale failed. The held units return to the available stock. */
    synchronized void release(String reservationId) {
        Reservation reservation = reservations.remove(reservationId);
        if (reservation != null) {
            giveBack(reservation);
        }
    }

    /** Releases every hold whose time is over. Returns the number of holds released. */
    synchronized int expire() {
        Instant now = clock.instant();
        List<Reservation> late = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            if (!reservation.expiresAt().isAfter(now)) {
                late.add(reservation);
            }
        }
        for (Reservation reservation : late) {
            reservations.remove(reservation.id());
            giveBack(reservation);
        }
        return late.size();
    }

    private void giveBack(Reservation reservation) {
        reservation.items().forEach((sku, qty) -> {
            reserved.merge(sku, -qty, Integer::sum);
            available.merge(sku, qty, Integer::sum);
        });
    }
}

final class Cart {
    private final Map<String, Integer> quantities = new TreeMap<>();
    private String couponCode;

    synchronized void add(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        quantities.merge(sku, quantity, Integer::sum);
    }

    synchronized void setQuantity(String sku, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        if (quantity == 0) {
            quantities.remove(sku);
        } else {
            quantities.put(sku, quantity);
        }
    }

    synchronized Map<String, Integer> snapshot() {
        return new TreeMap<>(quantities);
    }

    synchronized String couponCode() {
        return couponCode;
    }

    synchronized void useCoupon(String code) {
        this.couponCode = code;
    }

    synchronized void clear() {
        quantities.clear();
        couponCode = null;
    }
}

interface DiscountStrategy {
    long discountCents(List<OrderLine> lines);
}

final class PercentageDiscount implements DiscountStrategy {
    private final int percent;
    private final long capCents;

    PercentageDiscount(int percent, long capCents) {
        this.percent = percent;
        this.capCents = capCents;
    }

    @Override
    public long discountCents(List<OrderLine> lines) {
        long subtotal = 0;
        for (OrderLine line : lines) {
            subtotal += line.totalCents();
        }
        return Math.min(capCents, subtotal * percent / 100);
    }
}

final class FlatDiscount implements DiscountStrategy {
    private final long amountCents;

    FlatDiscount(long amountCents) {
        this.amountCents = amountCents;
    }

    @Override
    public long discountCents(List<OrderLine> lines) {
        return amountCents;
    }
}

final class CategoryDiscount implements DiscountStrategy {
    private final String category;
    private final int percent;

    CategoryDiscount(String category, int percent) {
        this.category = category;
        this.percent = percent;
    }

    @Override
    public long discountCents(List<OrderLine> lines) {
        long sum = 0;
        for (OrderLine line : lines) {
            if (line.category().equals(category)) {
                sum += line.totalCents();
            }
        }
        return sum * percent / 100;
    }
}

/** Every (x + 1)th unit of one product is free. */
final class BuyXGetOneFree implements DiscountStrategy {
    private final String sku;
    private final int x;

    BuyXGetOneFree(String sku, int x) {
        this.sku = sku;
        this.x = x;
    }

    @Override
    public long discountCents(List<OrderLine> lines) {
        long sum = 0;
        for (OrderLine line : lines) {
            if (line.sku().equals(sku)) {
                sum += (long) (line.quantity() / (x + 1)) * line.unitCents();
            }
        }
        return sum;
    }
}

final class Coupon {
    private final String code;
    private final DiscountStrategy discount;
    private final long minSubtotalCents;
    private final Instant expiresAt;
    private final AtomicInteger remainingUses;

    Coupon(String code, DiscountStrategy discount, long minSubtotalCents,
           Instant expiresAt, int maxUses) {
        this.code = code;
        this.discount = discount;
        this.minSubtotalCents = minSubtotalCents;
        this.expiresAt = expiresAt;
        this.remainingUses = new AtomicInteger(maxUses);
    }

    String code() {
        return code;
    }

    DiscountStrategy discount() {
        return discount;
    }

    /** Checks the rules that do not change the coupon. */
    void checkEligible(long subtotalCents, Instant now) {
        if (!expiresAt.isAfter(now)) {
            throw new CouponException("coupon " + code + " has expired");
        }
        if (subtotalCents < minSubtotalCents) {
            throw new CouponException("coupon " + code + " needs a subtotal of at least "
                    + Main.money(minSubtotalCents));
        }
        if (remainingUses.get() <= 0) {
            throw new CouponException("coupon " + code + " is used up");
        }
    }

    /** Atomic claim of one use. */
    boolean tryRedeem() {
        while (true) {
            int left = remainingUses.get();
            if (left <= 0) {
                return false;
            }
            if (remainingUses.compareAndSet(left, left - 1)) {
                return true;
            }
        }
    }

    void giveBack() {
        remainingUses.incrementAndGet();
    }
}

final class PricingService {
    private final List<DiscountStrategy> automatic;

    PricingService(List<DiscountStrategy> automatic) {
        this.automatic = List.copyOf(automatic);
    }

    PriceBreakdown price(List<OrderLine> lines, Coupon coupon, Instant now) {
        long subtotal = 0;
        for (OrderLine line : lines) {
            subtotal += line.totalCents();
        }
        long discount = 0;
        for (DiscountStrategy promotion : automatic) {
            discount += promotion.discountCents(lines);
        }
        if (coupon != null) {
            coupon.checkEligible(subtotal, now);
            discount += coupon.discount().discountCents(lines);
        }
        discount = Math.min(discount, subtotal);
        return new PriceBreakdown(subtotal, discount, subtotal - discount);
    }
}

enum OrderStatus {
    CREATED, PAID, SHIPPED, DELIVERED, CANCELLED;

    Set<OrderStatus> next() {
        return switch (this) {
            case CREATED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}

final class Order {
    private final String id;
    private final String userId;
    private final List<OrderLine> lines;
    private final PriceBreakdown price;
    private final String couponCode;
    private final List<OrderStatus> history = new ArrayList<>();
    private OrderStatus status = OrderStatus.CREATED;
    private String paymentRef;

    Order(String id, String userId, List<OrderLine> lines, PriceBreakdown price,
          String couponCode) {
        this.id = id;
        this.userId = userId;
        this.lines = List.copyOf(lines);
        this.price = price;
        this.couponCode = couponCode;
        this.history.add(status);
    }

    String id() {
        return id;
    }

    List<OrderLine> lines() {
        return lines;
    }

    PriceBreakdown price() {
        return price;
    }

    synchronized OrderStatus status() {
        return status;
    }

    synchronized List<OrderStatus> history() {
        return List.copyOf(history);
    }

    synchronized String paymentRef() {
        return paymentRef;
    }

    synchronized void moveTo(OrderStatus next) {
        if (!status.next().contains(next)) {
            throw new IllegalStateException(
                    "order " + id + " cannot move from " + status + " to " + next);
        }
        status = next;
        history.add(next);
    }

    /** Only a paid order can be cancelled by the customer. */
    synchronized void cancelPaid() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException(
                    "order " + id + " is " + status + ", only a PAID order can be cancelled");
        }
        moveTo(OrderStatus.CANCELLED);
    }

    synchronized void markPaid(String reference) {
        moveTo(OrderStatus.PAID);
        paymentRef = reference;
    }

    synchronized String describe() {
        return id + " " + status + " " + userId + " total " + Main.money(price.totalCents())
                + " items=" + lines.size() + (couponCode == null ? "" : " coupon=" + couponCode);
    }
}

record PaymentResult(boolean success, String reference, String message) {
}

interface PaymentGateway {
    PaymentResult charge(String idempotencyKey, long amountCents, String paymentToken);

    PaymentResult refund(String paymentReference, long amountCents);
}

/** A test gateway. It repeats the stored answer when it sees an idempotency key twice. */
final class FakeGateway implements PaymentGateway {
    private final Map<String, PaymentResult> answers = new HashMap<>();
    private int charges;
    private int refunds;
    private Runnable beforeCharge = () -> { };
    private boolean loseNextResponse;

    synchronized void beforeCharge(Runnable hook) {
        this.beforeCharge = hook;
    }

    /** The next charge succeeds, but the caller gets a timeout instead of the answer. */
    synchronized void loseNextResponse() {
        this.loseNextResponse = true;
    }

    synchronized int successfulCharges() {
        return charges;
    }

    synchronized int refunds() {
        return refunds;
    }

    @Override
    public synchronized PaymentResult charge(String key, long amountCents, String token) {
        PaymentResult known = answers.get(key);
        if (known != null) {
            return known;
        }
        beforeCharge.run();
        PaymentResult result;
        if (token.equals("tok_decline")) {
            result = new PaymentResult(false, null, "card declined");
        } else {
            charges++;
            result = new PaymentResult(true, "PAY-" + charges, "ok");
        }
        answers.put(key, result);
        if (loseNextResponse) {
            loseNextResponse = false;
            throw new PaymentTimeoutException(key);
        }
        return result;
    }

    @Override
    public synchronized PaymentResult refund(String paymentReference, long amountCents) {
        refunds++;
        return new PaymentResult(true, "RF-" + refunds, "refunded");
    }
}

final class ShopService {
    private final Map<String, Product> catalog = new ConcurrentHashMap<>();
    private final Map<String, Coupon> coupons = new ConcurrentHashMap<>();
    private final Map<String, Cart> carts = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Order>> checkouts = new ConcurrentHashMap<>();
    private final Inventory inventory;
    private final PricingService pricing;
    private final PaymentGateway gateway;
    private final Clock clock;
    private final AtomicLong orderSeq = new AtomicLong();

    ShopService(Inventory inventory, PricingService pricing, PaymentGateway gateway,
                Clock clock) {
        this.inventory = inventory;
        this.pricing = pricing;
        this.gateway = gateway;
        this.clock = clock;
    }

    Inventory inventory() {
        return inventory;
    }

    void addProduct(Product product, int stock) {
        catalog.put(product.sku(), product);
        inventory.restock(product.sku(), stock);
    }

    void addCoupon(Coupon coupon) {
        coupons.put(coupon.code(), coupon);
    }

    private Cart cart(String userId) {
        return carts.computeIfAbsent(userId, id -> new Cart());
    }

    void addToCart(String userId, String sku, int quantity) {
        requireProduct(sku);
        cart(userId).add(sku, quantity);
    }

    void setQuantity(String userId, String sku, int quantity) {
        requireProduct(sku);
        cart(userId).setQuantity(sku, quantity);
    }

    void removeFromCart(String userId, String sku) {
        cart(userId).setQuantity(sku, 0);
    }

    void applyCoupon(String userId, String code) {
        Coupon coupon = coupon(code);
        pricing.price(lines(cart(userId)), coupon, clock.instant());
        cart(userId).useCoupon(code);
    }

    PriceBreakdown quote(String userId) {
        Cart cart = cart(userId);
        return pricing.price(lines(cart), coupon(cart.couponCode()), clock.instant());
    }

    String cartText(String userId) {
        List<String> parts = new ArrayList<>();
        for (OrderLine line : lines(cart(userId))) {
            parts.add(line.sku() + " x" + line.quantity());
        }
        return String.join(", ", parts);
    }

    private Product requireProduct(String sku) {
        Product product = catalog.get(sku);
        if (product == null) {
            throw new ShopException("unknown product " + sku);
        }
        return product;
    }

    private Coupon coupon(String code) {
        if (code == null) {
            return null;
        }
        Coupon coupon = coupons.get(code);
        if (coupon == null) {
            throw new CouponException("unknown coupon " + code);
        }
        return coupon;
    }

    private List<OrderLine> lines(Cart cart) {
        List<OrderLine> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cart.snapshot().entrySet()) {
            Product product = requireProduct(entry.getKey());
            lines.add(new OrderLine(product.sku(), product.name(), product.category(),
                    product.priceCents(), entry.getValue()));
        }
        return lines;
    }

    Order checkout(String userId, String idempotencyKey, String paymentToken) {
        String scopedKey = userId + ":" + idempotencyKey;
        CompletableFuture<Order> mine = new CompletableFuture<>();
        CompletableFuture<Order> existing = checkouts.putIfAbsent(scopedKey, mine);
        if (existing != null) {
            return await(existing);
        }
        try {
            Order order = placeOrder(userId, scopedKey, paymentToken);
            mine.complete(order);
            return order;
        } catch (RuntimeException | Error e) {
            checkouts.remove(scopedKey, mine);
            mine.completeExceptionally(e);
            throw e;
        }
    }

    private static Order await(CompletableFuture<Order> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    private Order placeOrder(String userId, String scopedKey, String paymentToken) {
        Cart cart = cart(userId);
        List<OrderLine> lines = lines(cart);
        if (lines.isEmpty()) {
            throw new ShopException("the cart of " + userId + " is empty");
        }
        Coupon coupon = coupon(cart.couponCode());
        PriceBreakdown price = pricing.price(lines, coupon, clock.instant());
        Map<String, Integer> wanted = new TreeMap<>();
        for (OrderLine line : lines) {
            wanted.put(line.sku(), line.quantity());
        }
        Reservation hold = inventory.reserve(wanted);
        if (coupon != null && !coupon.tryRedeem()) {
            inventory.release(hold.id());
            throw new CouponException("coupon " + coupon.code() + " is used up");
        }
        Order order = new Order("O-" + orderSeq.incrementAndGet(), userId, lines, price,
                coupon == null ? null : coupon.code());
        orders.put(order.id(), order);

        PaymentResult payment;
        try {
            payment = gateway.charge(scopedKey, price.totalCents(), paymentToken);
        } catch (RuntimeException e) {
            // The outcome is unknown. Undo the local steps. A retry with the same key asks
            // the gateway again, and the gateway returns its first answer if it charged.
            abandon(order, hold, coupon);
            throw e;
        }
        if (!payment.success()) {
            abandon(order, hold, coupon);
            return order;
        }
        try {
            inventory.commit(hold.id());
        } catch (ReservationExpiredException e) {
            gateway.refund(payment.reference(), price.totalCents());
            abandon(order, hold, coupon);
            return order;
        }
        order.markPaid(payment.reference());
        cart.clear();
        return order;
    }

    private void abandon(Order order, Reservation hold, Coupon coupon) {
        inventory.release(hold.id());
        if (coupon != null) {
            coupon.giveBack();
        }
        order.moveTo(OrderStatus.CANCELLED);
    }

    void ship(String orderId) {
        order(orderId).moveTo(OrderStatus.SHIPPED);
    }

    void deliver(String orderId) {
        order(orderId).moveTo(OrderStatus.DELIVERED);
    }

    /** The customer cancels a paid order. The shop refunds and restocks. */
    void cancel(String orderId) {
        Order order = order(orderId);
        order.cancelPaid();
        gateway.refund(order.paymentRef(), order.price().totalCents());
        for (OrderLine line : order.lines()) {
            inventory.restock(line.sku(), line.quantity());
        }
    }

    private Order order(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new ShopException("unknown order " + orderId);
        }
        return order;
    }
}

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
