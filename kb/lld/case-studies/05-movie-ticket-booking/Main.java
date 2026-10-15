import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ManualClock clock = new ManualClock(Instant.parse("2026-10-30T09:00:00Z"));
        FakeGateway gateway = new FakeGateway(clock);
        BookingService service = new BookingService(
                clock, gateway, new TimeBasedRefund(), Duration.ofMinutes(5));

        // Catalog: one theater, two screens, two shows.
        Movie movie = new Movie("M1", "Inception", 148);
        Screen screen1 = Screen.grid("S1", "Screen 1", 4,
                new Row("A", SeatType.PREMIUM), new Row("B", SeatType.REGULAR));
        Screen screen2 = Screen.grid("S2", "Screen 2", 3, new Row("R", SeatType.REGULAR));
        Theater theater = new Theater("T1", "Galaxy Cinema", "Chennai", List.of(screen1, screen2));
        Map<SeatType, Money> prices = Map.of(
                SeatType.PREMIUM, new Money(1500), SeatType.REGULAR, new Money(1000));
        Instant start = Instant.parse("2026-10-31T18:00:00Z");
        service.addShow(new Show("SH-1", movie, theater, screen1, start, prices));
        service.addShow(new Show("SH-2", movie, theater, screen2, start, prices));

        System.out.println("== Search ==");
        for (Show show : service.findShows("inception", "chennai")) {
            System.out.println(show.describe());
        }
        System.out.println("Free seats SH-1: " + service.availableSeats("SH-1"));

        System.out.println("== Hold, pay, expire ==");
        step("alice holds A1,A2", () ->
                service.hold("alice", "SH-1", List.of("A1", "A2")).describe());
        step("bob holds A2,A3", () ->
                service.hold("bob", "SH-1", List.of("A2", "A3")).describe());
        step("bob holds A3,B1", () ->
                service.hold("bob", "SH-1", List.of("A3", "B1")).describe());
        step("alice pays B-1", () -> service.pay("B-1", "4111").describe());
        clock.advance(Duration.ofSeconds(301));
        step("bob pays B-2 after 301 s", () -> service.pay("B-2", "4111").describe());
        step("carol holds A3", () -> service.hold("carol", "SH-1", List.of("A3")).describe());
        step("carol pays B-3", () -> service.pay("B-3", "4111").describe());

        System.out.println("== Payment failures ==");
        step("dave holds B1", () -> service.hold("dave", "SH-1", List.of("B1")).describe());
        step("dave pays B-4 with a declined card", () -> service.pay("B-4", "4000").describe());
        step("dave retries B-4", () -> service.pay("B-4", "4111").describe());
        step("erin holds B2", () -> service.hold("erin", "SH-1", List.of("B2")).describe());
        gateway.setDelay(Duration.ofSeconds(400));
        step("erin pays B-5 on a slow gateway", () -> service.pay("B-5", "4111").describe());
        gateway.setDelay(Duration.ZERO);
        System.out.println("Refunds so far: " + gateway.refunds());
        System.out.println("Free seats SH-1: " + service.availableSeats("SH-1"));

        System.out.println("== Cancellation ==");
        step("alice cancels B-1 (more than 2 h before)", () -> service.cancel("B-1").describe());
        System.out.println("Free seats SH-1: " + service.availableSeats("SH-1"));
        clock.set(Instant.parse("2026-10-31T17:00:00Z"));
        step("dave cancels B-4 (1 h before)", () -> service.cancel("B-4").describe());
        step("alice cancels B-1 again", () -> service.cancel("B-1").describe());
        step("frank holds B2", () -> service.hold("frank", "SH-1", List.of("B2")).describe());
        clock.advance(Duration.ofSeconds(301));
        System.out.println("Sweep expired: " + service.expireLapsed().stream()
                .map(Booking::id).toList());
        System.out.println("Refunds: " + gateway.refunds());

        System.out.println("== Concurrency ==");
        UnsafeSeat unsafe = new UnsafeSeat();
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        AtomicInteger unsafeWins = new AtomicInteger();
        Runnable pause = () -> {
            try {
                bothChecked.await();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        runTogether(List.of(
                () -> { if (unsafe.hold("u1", pause)) unsafeWins.incrementAndGet(); },
                () -> { if (unsafe.hold("u2", pause)) unsafeWins.incrementAndGet(); }));
        System.out.println("No lock, 2 threads, 1 seat: " + unsafeWins + " holders");

        AtomicInteger won = new AtomicInteger();
        AtomicInteger lost = new AtomicInteger();
        List<Runnable> contenders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String user = "user-" + i;
            List<String> seats = i % 2 == 0 ? List.of("R2", "R3") : List.of("R3", "R2");
            contenders.add(() -> {
                try {
                    service.hold(user, "SH-2", seats);
                    won.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    lost.incrementAndGet();
                }
            });
        }
        runTogether(contenders);
        System.out.println("Per-show lock, 10 threads, seats R2+R3 in both orders: "
                + won + " won, " + lost + " lost");
        System.out.println("Free seats SH-2: " + service.availableSeats("SH-2"));
    }

    static void step(String label, Supplier<String> action) {
        try {
            System.out.println(label + ": " + action.get());
        } catch (BookingException e) {
            System.out.println(label + " -> rejected: " + e.getMessage());
        }
    }

    // Starts all tasks, releases them together, and joins them.
    static void runTogether(List<Runnable> tasks) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (Runnable task : tasks) {
            Thread thread = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    return;
                }
                task.run();
            });
            threads.add(thread);
            thread.start();
        }
        go.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
    }
}

// ---------- Value objects ----------

record Money(long minor) {
    static final Money ZERO = new Money(0);

    Money {
        if (minor < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    Money plus(Money other) {
        return new Money(Math.addExact(minor, other.minor));
    }

    boolean isZero() {
        return minor == 0;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d.%02d", minor / 100, minor % 100);
    }
}

enum SeatType { REGULAR, PREMIUM }

record Seat(String id, SeatType type) { }

record Row(String label, SeatType type) { }

record Movie(String id, String title, int durationMinutes) { }

// ---------- Catalog: theater, screen, show ----------

final class Screen {
    private final String id;
    private final String name;
    private final Map<String, Seat> seats = new LinkedHashMap<>();

    private Screen(String id, String name) {
        this.id = id;
        this.name = name;
    }

    static Screen grid(String id, String name, int seatsPerRow, Row... rows) {
        Screen screen = new Screen(id, name);
        for (Row row : rows) {
            for (int number = 1; number <= seatsPerRow; number++) {
                String seatId = row.label() + number;
                screen.seats.put(seatId, new Seat(seatId, row.type()));
            }
        }
        return screen;
    }

    String name() {
        return name;
    }

    Seat seat(String seatId) {
        return seats.get(seatId);
    }

    List<String> seatIds() {
        return List.copyOf(seats.keySet());
    }
}

record Theater(String id, String name, String city, List<Screen> screens) {
    Theater {
        screens = List.copyOf(screens);
    }
}

record Show(String id, Movie movie, Theater theater, Screen screen, Instant start,
            Map<SeatType, Money> prices) {
    Show {
        prices = Map.copyOf(prices);
    }

    Money priceOf(Seat seat) {
        return prices.get(seat.type());
    }

    String describe() {
        return id + " " + movie.title() + " " + start + " " + theater.name() + " "
                + screen.name();
    }
}

// ---------- Booking and its state machine ----------

enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, EXPIRED, CANCELLED;

    boolean canMoveTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next != PENDING_PAYMENT;
            case CONFIRMED -> next == CANCELLED;
            case EXPIRED, CANCELLED -> false;
        };
    }
}

record Booking(String id, String userId, String showId, List<String> seatIds, Money total,
               BookingStatus status, Instant holdExpiresAt, String paymentRef) {
    Booking {
        seatIds = List.copyOf(seatIds);
    }

    Booking moveTo(BookingStatus next) {
        if (!status.canMoveTo(next)) {
            throw new BookingException(
                    "Booking " + id + " cannot move from " + status + " to " + next);
        }
        return new Booking(id, userId, showId, seatIds, total, next, holdExpiresAt, paymentRef);
    }

    Booking paidWith(String reference) {
        Booking confirmed = moveTo(BookingStatus.CONFIRMED);
        return new Booking(id, userId, showId, seatIds, total, confirmed.status(),
                holdExpiresAt, reference);
    }

    String describe() {
        return id + " " + userId + " " + seatIds + " " + status + " " + total;
    }
}

class BookingException extends RuntimeException {
    BookingException(String message) {
        super(message);
    }
}

class SeatUnavailableException extends BookingException {
    SeatUnavailableException(List<String> seatIds) {
        super("Seats not available: " + seatIds);
    }
}

// ---------- Time, payment, refund: the three things the design injects ----------

interface TimeSource {
    Instant now();
}

final class ManualClock implements TimeSource {
    private final AtomicReference<Instant> now;

    ManualClock(Instant start) {
        this.now = new AtomicReference<>(start);
    }

    @Override
    public Instant now() {
        return now.get();
    }

    void advance(Duration delta) {
        now.updateAndGet(current -> current.plus(delta));
    }

    void set(Instant instant) {
        now.set(instant);
    }
}

record PaymentResult(boolean approved, String reference) { }

interface PaymentGateway {
    PaymentResult charge(String userId, Money amount, String card);

    void refund(String reference, Money amount);
}

final class FakeGateway implements PaymentGateway {
    private final ManualClock clock;
    private final AtomicInteger charges = new AtomicInteger();
    private final List<String> refunds = new CopyOnWriteArrayList<>();
    private volatile Duration delay = Duration.ZERO;

    FakeGateway(ManualClock clock) {
        this.clock = clock;
    }

    void setDelay(Duration delay) {
        this.delay = delay;
    }

    List<String> refunds() {
        return List.copyOf(refunds);
    }

    @Override
    public PaymentResult charge(String userId, Money amount, String card) {
        clock.advance(delay);   // a slow gateway: time passes while the call runs
        if (card.startsWith("4000")) {
            return new PaymentResult(false, null);
        }
        return new PaymentResult(true, "pay-" + charges.incrementAndGet());
    }

    @Override
    public void refund(String reference, Money amount) {
        refunds.add(reference + " " + amount);
    }
}

interface RefundPolicy {
    Money refund(Money paid, Instant showStart, Instant now);
}

final class TimeBasedRefund implements RefundPolicy {
    private static final Duration FULL_REFUND_WINDOW = Duration.ofHours(2);

    @Override
    public Money refund(Money paid, Instant showStart, Instant now) {
        boolean early = !now.plus(FULL_REFUND_WINDOW).isAfter(showStart);
        return early ? paid : Money.ZERO;
    }
}

// ---------- The concurrency unit: all reservations of one show ----------

final class ShowReservations {
    private static final int MAX_SEATS_PER_BOOKING = 6;

    private final Show show;
    private final TimeSource clock;
    private final Duration holdTtl;
    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final Map<String, String> bookingBySeat = new HashMap<>();

    ShowReservations(Show show, TimeSource clock, Duration holdTtl) {
        this.show = show;
        this.clock = clock;
        this.holdTtl = holdTtl;
    }

    Show show() {
        return show;
    }

    // All or nothing: every requested seat is free, or no seat changes.
    // The id supplier runs only after validation, so a rejected hold uses no booking id.
    synchronized Booking hold(Supplier<String> newBookingId, String userId,
                              List<String> seatIds) {
        expireLapsed();
        Instant now = clock.now();
        if (!now.isBefore(show.start())) {
            throw new BookingException("Show " + show.id() + " has started");
        }
        if (seatIds.isEmpty() || seatIds.size() > MAX_SEATS_PER_BOOKING
                || new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new BookingException("Choose 1 to " + MAX_SEATS_PER_BOOKING + " different seats");
        }
        Money total = Money.ZERO;
        for (String seatId : seatIds) {
            Seat seat = show.screen().seat(seatId);
            if (seat == null) {
                throw new BookingException("Unknown seat: " + seatId);
            }
            total = total.plus(show.priceOf(seat));
        }
        List<String> taken = seatIds.stream().filter(bookingBySeat::containsKey).sorted().toList();
        if (!taken.isEmpty()) {
            throw new SeatUnavailableException(taken);
        }
        String bookingId = newBookingId.get();
        Booking booking = new Booking(bookingId, userId, show.id(), seatIds, total,
                BookingStatus.PENDING_PAYMENT, now.plus(holdTtl), null);
        bookings.put(bookingId, booking);
        seatIds.forEach(seatId -> bookingBySeat.put(seatId, bookingId));
        return booking;
    }

    synchronized Booking requirePayable(String bookingId) {
        expireLapsed();
        Booking booking = find(bookingId);
        if (booking.status() != BookingStatus.PENDING_PAYMENT) {
            throw new BookingException(
                    "Booking " + bookingId + " is " + booking.status() + ", not payable");
        }
        return booking;
    }

    // Returns the booking unchanged if it is no longer pending (the hold lapsed, the user
    // cancelled, or another payment confirmed it first). The caller then refunds its charge.
    synchronized Booking confirm(String bookingId, String paymentRef) {
        expireLapsed();
        Booking booking = find(bookingId);
        if (booking.status() != BookingStatus.PENDING_PAYMENT) {
            return booking;
        }
        Booking confirmed = booking.paidWith(paymentRef);
        bookings.put(bookingId, confirmed);
        return confirmed;
    }

    synchronized Booking cancel(String bookingId) {
        expireLapsed();
        Booking booking = find(bookingId);
        Booking cancelled = booking.moveTo(BookingStatus.CANCELLED);
        bookings.put(bookingId, cancelled);
        release(booking);
        return cancelled;
    }

    // Lazy expiry: every operation calls this first, so no seat stays locked by a dead hold.
    synchronized List<Booking> expireLapsed() {
        Instant now = clock.now();
        List<Booking> lapsed = new ArrayList<>();
        for (Booking booking : List.copyOf(bookings.values())) {
            if (booking.status() == BookingStatus.PENDING_PAYMENT
                    && !now.isBefore(booking.holdExpiresAt())) {
                Booking expired = booking.moveTo(BookingStatus.EXPIRED);
                bookings.put(booking.id(), expired);
                release(booking);
                lapsed.add(expired);
            }
        }
        return lapsed;
    }

    synchronized List<String> availableSeatIds() {
        expireLapsed();
        return show.screen().seatIds().stream()
                .filter(seatId -> !bookingBySeat.containsKey(seatId)).toList();
    }

    private Booking find(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throw new BookingException("Unknown booking: " + bookingId);
        }
        return booking;
    }

    private void release(Booking booking) {
        for (String seatId : booking.seatIds()) {
            bookingBySeat.remove(seatId, booking.id());
        }
    }
}

record CancelResult(Booking booking, Money refund) {
    String describe() {
        return booking.describe() + ", refund " + refund;
    }
}

// ---------- Facade: the public API of the system ----------

final class BookingService {
    private final TimeSource clock;
    private final PaymentGateway gateway;
    private final RefundPolicy refundPolicy;
    private final Duration holdTtl;
    private final Map<String, ShowReservations> reservationsByShow = new ConcurrentHashMap<>();
    private final Map<String, String> showByBooking = new ConcurrentHashMap<>();
    private final AtomicInteger bookingSequence = new AtomicInteger();

    BookingService(TimeSource clock, PaymentGateway gateway, RefundPolicy refundPolicy,
                   Duration holdTtl) {
        this.clock = clock;
        this.gateway = gateway;
        this.refundPolicy = refundPolicy;
        this.holdTtl = holdTtl;
    }

    void addShow(Show show) {
        reservationsByShow.put(show.id(), new ShowReservations(show, clock, holdTtl));
    }

    List<Show> findShows(String movieTitle, String city) {
        return reservationsByShow.values().stream()
                .map(ShowReservations::show)
                .filter(show -> show.movie().title().equalsIgnoreCase(movieTitle)
                        && show.theater().city().equalsIgnoreCase(city))
                .sorted(Comparator.comparing(Show::start).thenComparing(Show::id))
                .toList();
    }

    List<String> availableSeats(String showId) {
        return reservationsOfShow(showId).availableSeatIds();
    }

    Booking hold(String userId, String showId, List<String> seatIds) {
        ShowReservations reservations = reservationsOfShow(showId);
        Booking booking = reservations.hold(
                () -> "B-" + bookingSequence.incrementAndGet(), userId, seatIds);
        showByBooking.put(booking.id(), showId);
        return booking;
    }

    // Three steps. The gateway call runs outside the show lock, because it is slow.
    Booking pay(String bookingId, String card) {
        ShowReservations reservations = reservationsOfBooking(bookingId);
        Booking booking = reservations.requirePayable(bookingId);
        PaymentResult result = gateway.charge(booking.userId(), booking.total(), card);
        if (!result.approved()) {
            throw new BookingException("Payment declined for " + bookingId);
        }
        Booking after = reservations.confirm(bookingId, result.reference());
        // Success only if this charge confirmed the booking. Compare the reference, not the
        // status: a second pay call that loses the race sees CONFIRMED with the other charge.
        if (!result.reference().equals(after.paymentRef())) {
            gateway.refund(result.reference(), booking.total());
            throw new BookingException("Booking " + bookingId + " is " + after.status()
                    + " after payment, charge refunded");
        }
        return after;
    }

    CancelResult cancel(String bookingId) {
        ShowReservations reservations = reservationsOfBooking(bookingId);
        Booking cancelled = reservations.cancel(bookingId);
        Money refund = Money.ZERO;
        if (cancelled.paymentRef() != null) {
            refund = refundPolicy.refund(
                    cancelled.total(), reservations.show().start(), clock.now());
            if (!refund.isZero()) {
                gateway.refund(cancelled.paymentRef(), refund);
            }
        }
        return new CancelResult(cancelled, refund);
    }

    // A scheduled job calls this in production. Lazy expiry already keeps seats correct.
    List<Booking> expireLapsed() {
        List<Booking> lapsed = new ArrayList<>();
        reservationsByShow.values().forEach(r -> lapsed.addAll(r.expireLapsed()));
        return lapsed;
    }

    private ShowReservations reservationsOfShow(String showId) {
        ShowReservations reservations = reservationsByShow.get(showId);
        if (reservations == null) {
            throw new BookingException("Unknown show: " + showId);
        }
        return reservations;
    }

    private ShowReservations reservationsOfBooking(String bookingId) {
        String showId = showByBooking.get(bookingId);
        if (showId == null) {
            throw new BookingException("Unknown booking: " + bookingId);
        }
        return reservationsOfShow(showId);
    }
}

// ---------- The wrong form, kept to show the race ----------

final class UnsafeSeat {
    private String owner;

    boolean hold(String user, Runnable betweenCheckAndAct) {
        if (owner != null) {
            return false;
        }
        betweenCheckAndAct.run();   // test hook: both threads pass the check first
        owner = user;
        return true;
    }
}
