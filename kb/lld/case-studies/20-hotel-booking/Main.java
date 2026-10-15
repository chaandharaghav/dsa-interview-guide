import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        ManualDates dates = new ManualDates(LocalDate.of(2026, 11, 1));
        PricingStrategy pricing = new WeekendSurcharge(new BasePricing(Map.of(
                RoomType.SINGLE, new Money(8_000),
                RoomType.DOUBLE, new Money(12_000),
                RoomType.SUITE, new Money(30_000))), 25);
        HotelService hotel = new HotelService(List.of(
                new Room("S1", RoomType.SINGLE), new Room("S2", RoomType.SINGLE),
                new Room("D1", RoomType.DOUBLE), new Room("D2", RoomType.DOUBLE),
                new Room("D3", RoomType.DOUBLE), new Room("SU1", RoomType.SUITE)),
                pricing, dates);
        CancellationPolicy flexible = new FlexibleCancellation();
        CancellationPolicy strict = new NonRefundable();

        System.out.println("== Availability and price ==");
        DateRange stay = range(10, 15);
        System.out.println("DOUBLE rooms free on each night of " + stay + ": "
                + hotel.availabilityByNight(RoomType.DOUBLE, stay).values());
        System.out.println("Quote DOUBLE " + range(10, 13) + ": "
                + hotel.quote(RoomType.DOUBLE, range(10, 13)));
        System.out.println("Quote DOUBLE " + range(12, 15) + " (Fri and Sat nights): "
                + hotel.quote(RoomType.DOUBLE, range(12, 15)));

        System.out.println("== Book ==");
        Booking alice = hotel.book("alice", RoomType.DOUBLE, range(10, 13), flexible);
        System.out.println("alice: " + alice.describe());
        Booking bob = hotel.book("bob", RoomType.DOUBLE, range(12, 15), flexible);
        System.out.println("bob: " + bob.describe());
        Booking carol = hotel.book("carol", RoomType.DOUBLE, range(13, 14), strict);
        System.out.println("carol: " + carol.describe());
        System.out.println("DOUBLE rooms free on each night of " + stay + ": "
                + hotel.availabilityByNight(RoomType.DOUBLE, stay).values());
        System.out.println("Rooms free for DOUBLE " + range(12, 14) + ": "
                + hotel.availableRooms(RoomType.DOUBLE, range(12, 14)));
        hotel.book("dave", RoomType.SUITE, range(10, 12), flexible);
        tryBook(hotel, "erin", RoomType.SUITE, range(11, 13), flexible);
        Booking back = hotel.book("frank", RoomType.SUITE, range(12, 13), flexible);
        System.out.println("frank gets the night that is free: " + back.describe());
        DateRange past = new DateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2));
        tryBook(hotel, "gina", RoomType.SINGLE, past, flexible);

        System.out.println("== Cancel ==");
        CancelResult early = hotel.cancel(alice.id());
        System.out.println("alice cancels 9 days before: refund " + early.refund() + " of "
                + early.booking().total());
        System.out.println("DOUBLE rooms free on each night of " + stay + ": "
                + hotel.availabilityByNight(RoomType.DOUBLE, stay).values());
        dates.set(LocalDate.of(2026, 11, 10));
        CancelResult late = hotel.cancel(bob.id());
        System.out.println("bob cancels 2 days before: refund " + late.refund() + " of "
                + late.booking().total());
        CancelResult strictResult = hotel.cancel(carol.id());
        System.out.println("carol cancels a non-refundable booking: refund "
                + strictResult.refund() + " of " + strictResult.booking().total());
        try {
            hotel.cancel(alice.id());
        } catch (BookingException e) {
            System.out.println("alice cancels again -> rejected: " + e.getMessage());
        }
        Booking again = hotel.book("hana", RoomType.DOUBLE, range(10, 13), flexible);
        System.out.println("hana gets the freed room: " + again.describe());

        System.out.println("== Check in and check out ==");
        Booking dave = hotel.bookingsOf("dave").get(0);
        System.out.println("dave checks in: " + hotel.checkIn(dave.id()).describe());
        try {
            hotel.cancel(dave.id());
        } catch (BookingException e) {
            System.out.println("dave cancels after check-in -> rejected: " + e.getMessage());
        }
        try {
            hotel.checkIn(frank(hotel).id());
        } catch (BookingException e) {
            System.out.println("frank checks in too early -> rejected: " + e.getMessage());
        }
        dates.set(LocalDate.of(2026, 11, 12));
        System.out.println("frank checks in: " + hotel.checkIn(frank(hotel).id()).describe());
        System.out.println("dave checks out: " + hotel.checkOut(dave.id()).describe());

        System.out.println("== Concurrency ==");
        runConcurrencyDemo(new ManualDates(LocalDate.of(2026, 11, 1)));
    }

    private static Booking frank(HotelService hotel) {
        return hotel.bookingsOf("frank").get(0);
    }

    private static void runConcurrencyDemo(ManualDates dates) throws Exception {
        UnsafeRoom unsafe = new UnsafeRoom();
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger unsafeWins = new AtomicInteger();
        runTogether(List.of(
                () -> unsafeReserve(unsafe, barrier, unsafeWins),
                () -> unsafeReserve(unsafe, barrier, unsafeWins)));
        System.out.println("No lock, 2 threads, 1 room, same dates: " + unsafeWins.get()
                + " bookings succeeded");

        HotelService hotel = new HotelService(List.of(
                new Room("D1", RoomType.DOUBLE), new Room("D2", RoomType.DOUBLE),
                new Room("D3", RoomType.DOUBLE)),
                new BasePricing(Map.of(RoomType.DOUBLE, new Money(12_000))), dates);
        AtomicInteger booked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Runnable> guests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String guest = "guest-" + i;
            guests.add(() -> {
                try {
                    hotel.book(guest, RoomType.DOUBLE, range(20, 23), new NonRefundable());
                    booked.incrementAndGet();
                } catch (BookingException e) {
                    rejected.incrementAndGet();
                }
            });
        }
        runTogether(guests);
        System.out.println("10 threads, 3 rooms, same dates: " + booked.get() + " booked, "
                + rejected.get() + " rejected");
        System.out.println("Rooms still free: " + hotel.availableRooms(RoomType.DOUBLE,
                range(20, 23)));

        HotelService suite = new HotelService(List.of(new Room("SU1", RoomType.SUITE)),
                new BasePricing(Map.of(RoomType.SUITE, new Money(30_000))), dates);
        int[][] requests = {{10, 12}, {11, 13}, {12, 14}, {10, 11}, {13, 15}, {11, 12}};
        List<Runnable> overlapping = new ArrayList<>();
        for (int[] r : requests) {
            overlapping.add(() -> {
                try {
                    suite.book("g", RoomType.SUITE, range(r[0], r[1]), new NonRefundable());
                } catch (BookingException e) {
                    // The other guests hold the room. The request is rejected.
                }
            });
        }
        runTogether(overlapping);
        List<Booking> held = suite.bookingsOf("g");
        boolean overlapFree = true;
        for (Booking a : held) {
            for (Booking b : held) {
                if (a != b && a.range().overlaps(b.range())) {
                    overlapFree = false;
                }
            }
        }
        System.out.println("6 overlapping requests, 1 room: at least one booked: "
                + !held.isEmpty() + ", no two bookings overlap: " + overlapFree);

        HotelService one = new HotelService(List.of(new Room("D1", RoomType.DOUBLE)),
                new BasePricing(Map.of(RoomType.DOUBLE, new Money(12_000))), dates);
        Booking target = one.book("zoe", RoomType.DOUBLE, range(20, 22),
                new FlexibleCancellation());
        AtomicInteger cancelled = new AtomicInteger();
        List<Runnable> cancels = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cancels.add(() -> {
                try {
                    one.cancel(target.id());
                    cancelled.incrementAndGet();
                } catch (BookingException e) {
                    // Another thread cancelled first.
                }
            });
        }
        runTogether(cancels);
        System.out.println("5 threads cancel one booking: " + cancelled.get()
                + " refund issued");
    }

    private static void unsafeReserve(UnsafeRoom room, CyclicBarrier barrier,
                                      AtomicInteger wins) {
        boolean ok = room.reserve(range(20, 23), () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        if (ok) {
            wins.incrementAndGet();
        }
    }

    private static void runTogether(List<Runnable> tasks) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (Runnable task : tasks) {
            Thread t = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                task.run();
            });
            threads.add(t);
            t.start();
        }
        go.countDown();
        for (Thread t : threads) {
            t.join();
        }
    }

    private static void tryBook(HotelService hotel, String guest, RoomType type,
                                DateRange range, CancellationPolicy policy) {
        try {
            hotel.book(guest, type, range, policy);
        } catch (BookingException e) {
            System.out.println(guest + " -> rejected: " + e.getMessage());
        }
    }

    private static DateRange range(int fromDay, int toDay) {
        return new DateRange(LocalDate.of(2026, 11, fromDay), LocalDate.of(2026, 11, toDay));
    }
}

interface DateSource {
    LocalDate today();
}

final class ManualDates implements DateSource {
    private volatile LocalDate today;

    ManualDates(LocalDate start) {
        this.today = start;
    }

    void set(LocalDate date) {
        this.today = date;
    }

    @Override
    public LocalDate today() {
        return today;
    }
}

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

    // Rounds down to a whole minor unit.
    Money percent(int percent) {
        return new Money(minor * percent / 100);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d.%02d", minor / 100, minor % 100);
    }
}

enum RoomType { SINGLE, DOUBLE, SUITE }

// A stay: the guest sleeps the nights from checkIn up to, but not including, checkOut.
record DateRange(LocalDate checkIn, LocalDate checkOut) {
    DateRange {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }

    List<LocalDate> nights() {
        return checkIn.datesUntil(checkOut).toList();
    }

    @Override
    public String toString() {
        return checkIn + ".." + checkOut;
    }
}

final class BookingException extends RuntimeException {
    BookingException(String message) {
        super(message);
    }
}

enum BookingStatus {
    CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    boolean canMoveTo(BookingStatus next) {
        return switch (this) {
            case CONFIRMED -> next == CHECKED_IN || next == CANCELLED;
            case CHECKED_IN -> next == CHECKED_OUT;
            case CHECKED_OUT, CANCELLED -> false;
        };
    }
}

record Booking(String id, String guestId, String roomId, RoomType type, DateRange range,
               Money total, CancellationPolicy policy, BookingStatus status) {
    Booking moveTo(BookingStatus next) {
        if (!status.canMoveTo(next)) {
            throw new BookingException(
                    "Booking " + id + " cannot move from " + status + " to " + next);
        }
        return new Booking(id, guestId, roomId, type, range, total, policy, next);
    }

    String describe() {
        return id + " " + guestId + " " + roomId + " " + range + " " + status + " " + total;
    }
}

interface PricingStrategy {
    Money priceFor(RoomType type, LocalDate night);
}

final class BasePricing implements PricingStrategy {
    private final Map<RoomType, Money> nightlyRate;

    BasePricing(Map<RoomType, Money> nightlyRate) {
        this.nightlyRate = new EnumMap<>(nightlyRate);
    }

    @Override
    public Money priceFor(RoomType type, LocalDate night) {
        return nightlyRate.get(type);
    }
}

// Decorator: adds a percentage on Friday and Saturday nights to any other strategy.
final class WeekendSurcharge implements PricingStrategy {
    private final PricingStrategy inner;
    private final int surchargePercent;

    WeekendSurcharge(PricingStrategy inner, int surchargePercent) {
        this.inner = inner;
        this.surchargePercent = surchargePercent;
    }

    @Override
    public Money priceFor(RoomType type, LocalDate night) {
        Money base = inner.priceFor(type, night);
        DayOfWeek day = night.getDayOfWeek();
        boolean weekend = day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
        return weekend ? base.plus(base.percent(surchargePercent)) : base;
    }
}

interface CancellationPolicy {
    Money refund(Money paid, LocalDate checkIn, LocalDate today);
}

final class FlexibleCancellation implements CancellationPolicy {
    @Override
    public Money refund(Money paid, LocalDate checkIn, LocalDate today) {
        long daysBefore = ChronoUnit.DAYS.between(today, checkIn);
        if (daysBefore >= 3) {
            return paid;
        }
        return daysBefore >= 1 ? paid.percent(50) : Money.ZERO;
    }
}

final class NonRefundable implements CancellationPolicy {
    @Override
    public Money refund(Money paid, LocalDate checkIn, LocalDate today) {
        return Money.ZERO;
    }
}

record CancelResult(Booking booking, Money refund) { }

// One physical room. The room owns its calendar and its lock.
final class Room {
    private final String id;
    private final RoomType type;
    private final TreeMap<LocalDate, DateRange> stays = new TreeMap<>();   // by check-in

    Room(String id, RoomType type) {
        this.id = id;
        this.type = type;
    }

    String id() {
        return id;
    }

    RoomType type() {
        return type;
    }

    // Check and take in one step: this method is the answer to the double booking.
    synchronized boolean tryReserve(DateRange range) {
        if (!isFree(range)) {
            return false;
        }
        stays.put(range.checkIn(), range);
        return true;
    }

    synchronized boolean release(DateRange range) {
        return stays.remove(range.checkIn(), range);
    }

    synchronized boolean isFree(DateRange range) {
        // The stays do not overlap, so only the last stay that starts before our
        // check-out can reach into our range.
        Map.Entry<LocalDate, DateRange> last = stays.lowerEntry(range.checkOut());
        return last == null || !last.getValue().checkOut().isAfter(range.checkIn());
    }

    synchronized boolean isFreeOn(LocalDate night) {
        Map.Entry<LocalDate, DateRange> last = stays.floorEntry(night);
        return last == null || !last.getValue().checkOut().isAfter(night);
    }
}

final class HotelService {
    private final Map<RoomType, List<Room>> roomsByType = new EnumMap<>(RoomType.class);
    private final Map<String, Room> roomsById = new HashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final AtomicInteger bookingSeq = new AtomicInteger();
    private final PricingStrategy pricing;
    private final DateSource dates;

    HotelService(List<Room> rooms, PricingStrategy pricing, DateSource dates) {
        List<Room> ordered = new ArrayList<>(rooms);
        ordered.sort(Comparator.comparing(Room::id));
        for (Room room : ordered) {
            roomsByType.computeIfAbsent(room.type(), t -> new ArrayList<>()).add(room);
            roomsById.put(room.id(), room);
        }
        this.pricing = pricing;
        this.dates = dates;
    }

    Money quote(RoomType type, DateRange range) {
        Money total = Money.ZERO;
        for (LocalDate night : range.nights()) {
            total = total.plus(pricing.priceFor(type, night));
        }
        return total;
    }

    // Advisory: each room is read on its own, so the answer can be old when the caller acts.
    List<String> availableRooms(RoomType type, DateRange range) {
        List<String> free = new ArrayList<>();
        for (Room room : roomsOf(type)) {
            if (room.isFree(range)) {
                free.add(room.id());
            }
        }
        return free;
    }

    Map<LocalDate, Integer> availabilityByNight(RoomType type, DateRange range) {
        Map<LocalDate, Integer> freeCount = new TreeMap<>();
        for (LocalDate night : range.nights()) {
            int count = 0;
            for (Room room : roomsOf(type)) {
                if (room.isFreeOn(night)) {
                    count++;
                }
            }
            freeCount.put(night, count);
        }
        return freeCount;
    }

    Booking book(String guestId, RoomType type, DateRange range, CancellationPolicy policy) {
        if (range.checkIn().isBefore(dates.today())) {
            throw new BookingException("Check-in " + range.checkIn() + " is in the past");
        }
        Money total = quote(type, range);
        for (Room room : roomsOf(type)) {
            if (room.tryReserve(range)) {
                Booking booking = new Booking("H-" + bookingSeq.incrementAndGet(), guestId,
                        room.id(), type, range, total, policy, BookingStatus.CONFIRMED);
                bookings.put(booking.id(), booking);
                return booking;
            }
        }
        throw new BookingException("No " + type + " room is free for " + range);
    }

    CancelResult cancel(String bookingId) {
        // computeIfPresent runs the lambda for one key at a time, so only one cancel wins.
        Booking cancelled = found(bookingId, bookings.computeIfPresent(bookingId,
                (id, booking) -> booking.moveTo(BookingStatus.CANCELLED)));
        roomsById.get(cancelled.roomId()).release(cancelled.range());
        Money refund = cancelled.policy()
                .refund(cancelled.total(), cancelled.range().checkIn(), dates.today());
        return new CancelResult(cancelled, refund);
    }

    Booking checkIn(String bookingId) {
        return found(bookingId, bookings.computeIfPresent(bookingId, (id, booking) -> {
            if (dates.today().isBefore(booking.range().checkIn())) {
                throw new BookingException("Too early to check in " + id);
            }
            return booking.moveTo(BookingStatus.CHECKED_IN);
        }));
    }

    Booking checkOut(String bookingId) {
        return found(bookingId, bookings.computeIfPresent(bookingId,
                (id, booking) -> booking.moveTo(BookingStatus.CHECKED_OUT)));
    }

    List<Booking> bookingsOf(String guestId) {
        List<Booking> found = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            if (booking.guestId().equals(guestId) && booking.status() != BookingStatus.CANCELLED) {
                found.add(booking);
            }
        }
        found.sort(Comparator.comparing(Booking::id));
        return found;
    }

    private List<Room> roomsOf(RoomType type) {
        return roomsByType.getOrDefault(type, List.of());
    }

    // computeIfPresent returns null if the id is unknown. Turn that into a clear error.
    private static Booking found(String bookingId, Booking updated) {
        if (updated == null) {
            throw new BookingException("Unknown booking " + bookingId);
        }
        return updated;
    }
}

// The wrong form: the check and the write are two steps. Kept for the demo only.
final class UnsafeRoom {
    private final List<DateRange> stays = new CopyOnWriteArrayList<>();

    boolean reserve(DateRange range, Runnable betweenCheckAndAct) {
        for (DateRange stay : stays) {
            if (stay.overlaps(range)) {
                return false;
            }
        }
        betweenCheckAndAct.run();   // test hook: both threads pass the check first
        stays.add(range);
        return true;
    }
}
