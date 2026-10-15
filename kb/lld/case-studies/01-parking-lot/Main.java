import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-05T08:00:00Z"));

        System.out.println("== 1. Park vehicles with the best-fit strategy ==");
        ParkingLot lot = new ParkingLot(
                List.of(ParkingFloor.of(0, SpotType.SMALL, SpotType.SMALL, SpotType.MEDIUM,
                                SpotType.MEDIUM, SpotType.LARGE),
                        ParkingFloor.of(1, SpotType.MEDIUM, SpotType.LARGE)),
                new BestFitStrategy(), HourlyPricing.standard(), clock);
        DisplayBoard board0 = new DisplayBoard(lot.floor(0));
        DisplayBoard board1 = new DisplayBoard(lot.floor(1));
        lot.addListener(board0);
        lot.addListener(board1);
        EntryGate entry = new EntryGate("ENTRY-A", lot);
        ExitGate exit = new ExitGate("EXIT-A", lot);
        System.out.println(board0.text());
        System.out.println(board1.text());

        Ticket bike = entry.enter(new Vehicle("BIKE-1", VehicleType.MOTORCYCLE));
        Ticket car1 = entry.enter(new Vehicle("CAR-1", VehicleType.CAR));
        Ticket truck1 = entry.enter(new Vehicle("TRUCK-1", VehicleType.TRUCK));
        Ticket truck2 = entry.enter(new Vehicle("TRUCK-2", VehicleType.TRUCK));
        for (Ticket t : List.of(bike, car1, truck1, truck2)) {
            System.out.println(t.id() + " " + t.vehicle().plate() + " -> " + t.spot().id()
                    + " (" + t.spot().type() + ")");
        }
        tryEnter(entry, new Vehicle("TRUCK-3", VehicleType.TRUCK));
        System.out.println(board0.text());
        System.out.println(board1.text());

        System.out.println("== 2. Reject a duplicate plate, fill the medium spots ==");
        tryEnter(entry, new Vehicle("CAR-1", VehicleType.CAR));
        Ticket car2 = entry.enter(new Vehicle("CAR-2", VehicleType.CAR));
        Ticket car3 = entry.enter(new Vehicle("CAR-3", VehicleType.CAR));
        System.out.println(car2.vehicle().plate() + " -> " + car2.spot().id());
        System.out.println(car3.vehicle().plate() + " -> " + car3.spot().id());
        tryEnter(entry, new Vehicle("CAR-4", VehicleType.CAR));

        System.out.println("== 3. Pay and exit ==");
        clock.advance(Duration.ofMinutes(130));
        Receipt r1 = exit.leave(car1.id(), new CashPayment(new BigDecimal("100")));
        System.out.println(r1);
        System.out.println(board0.text());
        tryLeave(exit, car1.id(), new CashPayment(new BigDecimal("100")));
        tryLeave(exit, truck1.id(), new CardPayment("4242", new BigDecimal("100")));
        System.out.println("TRUCK-1 still parked: " + lot.isParked("TRUCK-1"));
        Receipt r2 = exit.leave(truck1.id(), new CardPayment("4242", new BigDecimal("500")));
        System.out.println(r2);
        Ticket car4 = entry.enter(new Vehicle("CAR-4", VehicleType.CAR));
        System.out.println("CAR-4 reuses " + car4.spot().id());

        System.out.println("== 4. Compare allocation strategies ==");
        for (SpotAllocationStrategy strategy
                : List.of(new FirstFitStrategy(), new BestFitStrategy())) {
            ParkingLot small = new ParkingLot(
                    List.of(ParkingFloor.of(0, SpotType.LARGE, SpotType.MEDIUM, SpotType.SMALL)),
                    strategy, HourlyPricing.standard(), clock);
            Ticket t = small.park(new Vehicle("BIKE-9", VehicleType.MOTORCYCLE));
            System.out.println(strategy.getClass().getSimpleName() + ": " + t.spot().id()
                    + " (" + t.spot().type() + ")");
        }

        System.out.println("== 5. Two gates assign spots at the same time ==");
        ParkingLot busy = new ParkingLot(
                List.of(ParkingFloor.of(0, SpotType.MEDIUM, SpotType.MEDIUM, SpotType.MEDIUM)),
                new BestFitStrategy(), HourlyPricing.standard(), clock);
        DisplayBoard busyBoard = new DisplayBoard(busy.floor(0));
        busy.addListener(busyBoard);
        List<EntryGate> gates = List.of(new EntryGate("G1", busy), new EntryGate("G2", busy));
        AtomicInteger parked = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        Set<String> spotIds = ConcurrentHashMap.newKeySet();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int n = i;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    Ticket t = gates.get(n % 2).enter(new Vehicle("RACE-" + n, VehicleType.CAR));
                    spotIds.add(t.spot().id());
                    parked.incrementAndGet();
                } catch (LotFullException e) {
                    rejected.incrementAndGet();
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
        System.out.println("parked=" + parked.get() + " rejected=" + rejected.get()
                + " distinctSpots=" + spotIds.size());
        System.out.println(busyBoard.text());
    }

    private static void tryEnter(EntryGate gate, Vehicle vehicle) {
        try {
            gate.enter(vehicle);
        } catch (ParkingException e) {
            System.out.println("rejected " + vehicle.plate() + ": " + e.getMessage());
        }
    }

    private static void tryLeave(ExitGate gate, String ticketId, PaymentMethod method) {
        try {
            gate.leave(ticketId, method);
        } catch (ParkingException e) {
            System.out.println("exit refused for " + ticketId + ": " + e.getMessage());
        }
    }
}

enum SpotType { SMALL, MEDIUM, LARGE }

enum VehicleType {
    MOTORCYCLE(SpotType.SMALL), CAR(SpotType.MEDIUM), TRUCK(SpotType.LARGE);

    private final SpotType smallestSpot;

    VehicleType(SpotType smallestSpot) {
        this.smallestSpot = smallestSpot;
    }

    boolean fitsIn(SpotType spot) {
        return spot.ordinal() >= smallestSpot.ordinal();
    }
}

record Vehicle(String plate, VehicleType type) {
    Vehicle {
        Objects.requireNonNull(plate, "plate");
        Objects.requireNonNull(type, "type");
    }
}

class ParkingException extends RuntimeException {
    ParkingException(String message) {
        super(message);
    }
}

class LotFullException extends ParkingException {
    LotFullException(Vehicle vehicle) {
        super("no free " + vehicle.type() + " spot");
    }
}

class InvalidTicketException extends ParkingException {
    InvalidTicketException(String ticketId) {
        super("ticket " + ticketId + " is unknown or already used");
    }
}

class PaymentFailedException extends ParkingException {
    PaymentFailedException(BigDecimal fee) {
        super("payment of " + fee + " was declined");
    }
}

final class ParkingSpot {
    private final String id;
    private final int floor;
    private final SpotType type;
    private final AtomicReference<Vehicle> occupant = new AtomicReference<>();

    ParkingSpot(String id, int floor, SpotType type) {
        this.id = id;
        this.floor = floor;
        this.type = type;
    }

    String id() {
        return id;
    }

    int floor() {
        return floor;
    }

    SpotType type() {
        return type;
    }

    boolean isFree() {
        return occupant.get() == null;
    }

    /** Atomic claim. Exactly one caller wins when several call at the same time. */
    boolean tryPark(Vehicle vehicle) {
        return occupant.compareAndSet(null, vehicle);
    }

    void release() {
        occupant.set(null);
    }
}

final class ParkingFloor {
    private final int number;
    private final List<ParkingSpot> spots;

    private ParkingFloor(int number, List<ParkingSpot> spots) {
        this.number = number;
        this.spots = spots;
    }

    static ParkingFloor of(int number, SpotType... types) {
        List<ParkingSpot> spots = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            spots.add(new ParkingSpot("F" + number + "-S" + (i + 1), number, types[i]));
        }
        return new ParkingFloor(number, Collections.unmodifiableList(spots));
    }

    int number() {
        return number;
    }

    List<ParkingSpot> spots() {
        return spots;
    }

    int freeCount(SpotType type) {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (spot.type() == type && spot.isFree()) {
                count++;
            }
        }
        return count;
    }

    String summary() {
        StringBuilder sb = new StringBuilder("Floor " + number);
        for (SpotType type : SpotType.values()) {
            sb.append(" | ").append(type).append(' ').append(freeCount(type));
        }
        return sb.toString();
    }
}

interface SpotAllocationStrategy {
    Optional<ParkingSpot> select(List<ParkingFloor> floors, Vehicle vehicle);
}

/** Takes the first free spot that fits, scanning floors from the entrance upward. */
final class FirstFitStrategy implements SpotAllocationStrategy {
    @Override
    public Optional<ParkingSpot> select(List<ParkingFloor> floors, Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            for (ParkingSpot spot : floor.spots()) {
                if (spot.isFree() && vehicle.type().fitsIn(spot.type())) {
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }
}

/** Takes the smallest free spot that fits. Large spots stay free for large vehicles. */
final class BestFitStrategy implements SpotAllocationStrategy {
    @Override
    public Optional<ParkingSpot> select(List<ParkingFloor> floors, Vehicle vehicle) {
        ParkingSpot best = null;
        for (ParkingFloor floor : floors) {
            for (ParkingSpot spot : floor.spots()) {
                if (spot.isFree() && vehicle.type().fitsIn(spot.type())
                        && (best == null || spot.type().ordinal() < best.type().ordinal())) {
                    best = spot;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}

record Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entryTime) { }

record Receipt(String ticketId, String plate, String spotId, long minutes, BigDecimal fee,
               String method) { }

interface PricingStrategy {
    BigDecimal fee(VehicleType type, Duration parked);
}

final class HourlyPricing implements PricingStrategy {
    private final Map<VehicleType, BigDecimal> ratePerHour;

    HourlyPricing(Map<VehicleType, BigDecimal> ratePerHour) {
        this.ratePerHour = new EnumMap<>(ratePerHour);
    }

    static HourlyPricing standard() {
        return new HourlyPricing(Map.of(VehicleType.MOTORCYCLE, new BigDecimal("10"),
                VehicleType.CAR, new BigDecimal("20"), VehicleType.TRUCK, new BigDecimal("50")));
    }

    @Override
    public BigDecimal fee(VehicleType type, Duration parked) {
        long billedHours = Math.max(1, (parked.toMinutes() + 59) / 60);
        return ratePerHour.get(type).multiply(BigDecimal.valueOf(billedHours));
    }
}

interface PaymentMethod {
    boolean pay(BigDecimal amount);

    String describe();
}

record CashPayment(BigDecimal tendered) implements PaymentMethod {
    @Override
    public boolean pay(BigDecimal amount) {
        return tendered.compareTo(amount) >= 0;
    }

    @Override
    public String describe() {
        return "cash";
    }
}

record CardPayment(String last4, BigDecimal limit) implements PaymentMethod {
    @Override
    public boolean pay(BigDecimal amount) {
        return limit.compareTo(amount) >= 0;
    }

    @Override
    public String describe() {
        return "card ending " + last4;
    }
}

interface AvailabilityListener {
    void onChange(ParkingFloor floor);
}

/** A display board for one floor. It reads the live counts when the lot notifies it. */
final class DisplayBoard implements AvailabilityListener {
    private final ParkingFloor floor;
    private String text;

    DisplayBoard(ParkingFloor floor) {
        this.floor = floor;
        this.text = floor.summary();
    }

    @Override
    public synchronized void onChange(ParkingFloor changed) {
        if (changed == floor) {
            text = floor.summary();
        }
    }

    synchronized String text() {
        return text;
    }
}

final class ParkingLot {
    private final List<ParkingFloor> floors;
    private final SpotAllocationStrategy allocation;
    private final PricingStrategy pricing;
    private final Clock clock;
    private final AtomicLong ticketSeq = new AtomicLong();
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();
    private final Set<String> parkedPlates = ConcurrentHashMap.newKeySet();
    private final List<AvailabilityListener> listeners = new CopyOnWriteArrayList<>();

    ParkingLot(List<ParkingFloor> floors, SpotAllocationStrategy allocation,
               PricingStrategy pricing, Clock clock) {
        this.floors = List.copyOf(floors);
        for (int i = 0; i < this.floors.size(); i++) {
            if (this.floors.get(i).number() != i) {
                throw new IllegalArgumentException("floor " + i + " has number "
                        + this.floors.get(i).number());
            }
        }
        this.allocation = allocation;
        this.pricing = pricing;
        this.clock = clock;
    }

    ParkingFloor floor(int number) {
        return floors.get(number);
    }

    void addListener(AvailabilityListener listener) {
        listeners.add(listener);
    }

    boolean isParked(String plate) {
        return parkedPlates.contains(plate);
    }

    Ticket park(Vehicle vehicle) {
        if (!parkedPlates.add(vehicle.plate())) {
            throw new ParkingException("vehicle " + vehicle.plate() + " is already parked");
        }
        ParkingSpot spot;
        try {
            spot = allocate(vehicle);
        } catch (RuntimeException e) {
            parkedPlates.remove(vehicle.plate()); // a failing strategy must not leave the plate
            throw e;
        }
        if (spot == null) {
            parkedPlates.remove(vehicle.plate());
            throw new LotFullException(vehicle);
        }
        Ticket ticket = new Ticket("T-" + ticketSeq.incrementAndGet(), vehicle, spot,
                clock.instant());
        activeTickets.put(ticket.id(), ticket);
        notifyListeners(spot);
        return ticket;
    }

    private ParkingSpot allocate(Vehicle vehicle) {
        while (true) {
            Optional<ParkingSpot> candidate = allocation.select(floors, vehicle);
            if (candidate.isEmpty()) {
                return null;
            }
            if (candidate.get().tryPark(vehicle)) {
                return candidate.get();
            }
            // Another gate took this spot between select and tryPark. Select again.
        }
    }

    Receipt exit(String ticketId, PaymentMethod method) {
        Ticket ticket = activeTickets.remove(ticketId);
        if (ticket == null) {
            throw new InvalidTicketException(ticketId);
        }
        boolean paid = false;
        try {
            Duration parked = Duration.between(ticket.entryTime(), clock.instant());
            BigDecimal fee = pricing.fee(ticket.vehicle().type(), parked);
            if (!method.pay(fee)) {
                throw new PaymentFailedException(fee);
            }
            paid = true;
            ticket.spot().release();
            parkedPlates.remove(ticket.vehicle().plate());
            notifyListeners(ticket.spot());
            return new Receipt(ticketId, ticket.vehicle().plate(), ticket.spot().id(),
                    parked.toMinutes(), fee, method.describe());
        } finally {
            if (!paid) {
                // The payment was declined or threw. The driver can try again.
                activeTickets.put(ticketId, ticket);
            }
        }
    }

    private void notifyListeners(ParkingSpot spot) {
        ParkingFloor floor = floors.get(spot.floor());
        for (AvailabilityListener listener : listeners) {
            listener.onChange(floor);
        }
    }
}

final class EntryGate {
    private final String id;
    private final ParkingLot lot;

    EntryGate(String id, ParkingLot lot) {
        this.id = id;
        this.lot = lot;
    }

    String id() {
        return id;
    }

    Ticket enter(Vehicle vehicle) {
        return lot.park(vehicle);
    }
}

final class ExitGate {
    private final String id;
    private final ParkingLot lot;

    ExitGate(String id, ParkingLot lot) {
        this.id = id;
        this.lot = lot;
    }

    String id() {
        return id;
    }

    Receipt leave(String ticketId, PaymentMethod method) {
        return lot.exit(ticketId, method);
    }
}

/** A clock that the demo moves by hand, so the fees are the same on every run. */
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
