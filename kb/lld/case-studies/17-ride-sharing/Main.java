import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-02T08:00:00Z"));
        RideService service = new RideService(new NearestDriverStrategy(5_000),
                new SurgeFare(new MeteredFare(MeteredFare.standardRates())),
                new DemandSupplySurge(250), clock, Duration.ofMinutes(10));

        Driver asha = new Driver("D1", "Asha", VehicleClass.ECONOMY);
        Driver ben = new Driver("D2", "Ben", VehicleClass.ECONOMY);
        Driver chen = new Driver("D3", "Chen", VehicleClass.ECONOMY);
        Driver dina = new Driver("D4", "Dina", VehicleClass.PREMIUM);
        for (Driver driver : List.of(asha, ben, chen, dina)) {
            service.registerDriver(driver);
        }
        service.goOnline("D1", new Location(500, 0));
        service.goOnline("D2", new Location(300, 400));
        service.goOnline("D3", new Location(2_000, 0));
        service.goOnline("D4", new Location(100, 100));
        asha.ratings().add(4);
        ben.ratings().add(5);
        ben.ratings().add(5);

        Rider ravi = new Rider("R1", "Ravi");
        Location origin = new Location(0, 0);
        Location destination = new Location(3_000, 2_000);

        System.out.println("== 1. Match, ride, pay, rate ==");
        System.out.println("estimate ECONOMY: " + money(
                service.estimateFare(origin, destination, VehicleClass.ECONOMY)));
        Trip trip = service.requestRide(ravi, origin, destination, VehicleClass.ECONOMY);
        System.out.println(service.describe(trip));
        clock.advance(Duration.ofMinutes(3));
        service.arrive(trip.id());
        clock.advance(Duration.ofMinutes(1));
        service.start(trip.id());
        clock.advance(Duration.ofMinutes(14));
        service.complete(trip.id());
        System.out.println(service.describe(trip));
        System.out.println("Asha is " + asha.status() + " at " + asha.location());
        service.rateDriver(trip.id(), 5);
        service.rateRider(trip.id(), 4);
        System.out.println("Asha rating " + asha.ratings().describe()
                + ", Ravi rating " + ravi.ratings().describe());
        attempt("rate the driver again", () -> service.rateDriver(trip.id(), 1));

        System.out.println("== 2. Compare matching strategies ==");
        RideRequest request = new RideRequest(ravi, origin, destination, VehicleClass.ECONOMY);
        service.goOnline("D1", new Location(500, 0));
        List<Driver> candidates = service.availableDrivers(VehicleClass.ECONOMY);
        MatchingStrategy nearest = new NearestDriverStrategy(5_000);
        MatchingStrategy topRated = new TopRatedStrategy(5_000);
        System.out.println("nearest: " + nearest.select(request, candidates).orElseThrow().name());
        System.out.println("topRated: "
                + topRated.select(request, candidates).orElseThrow().name());
        MatchingStrategy tight = new NearestDriverStrategy(400);
        System.out.println("nearest within 400 m: " + tight.select(request, candidates)
                .map(Driver::name).orElse("nobody"));

        System.out.println("== 3. Cancel ==");
        Trip second = service.requestRide(ravi, origin, destination, VehicleClass.ECONOMY);
        System.out.println(service.describe(second));
        service.cancel(second.id());
        System.out.println(service.describe(second));
        System.out.println("driver free again: "
                + service.availableDrivers(VehicleClass.ECONOMY).size() + " ECONOMY available");
        attempt("start a cancelled trip", () -> service.start(second.id()));

        System.out.println("== 4. Surge pricing ==");
        MutableClock surgeClock = new MutableClock(Instant.parse("2026-11-02T18:00:00Z"));
        RideService busy = new RideService(new NearestDriverStrategy(5_000),
                new SurgeFare(new MeteredFare(MeteredFare.standardRates())),
                new DemandSupplySurge(250), surgeClock, Duration.ofMinutes(10));
        busy.registerDriver(new Driver("D1", "Asha", VehicleClass.ECONOMY));
        busy.registerDriver(new Driver("D2", "Ben", VehicleClass.ECONOMY));
        busy.goOnline("D1", new Location(500, 0));
        busy.goOnline("D2", new Location(300, 400));
        for (String name : List.of("Ana", "Bo", "Cy")) {
            Rider rider = new Rider("R-" + name, name);
            try {
                Trip t = busy.requestRide(rider, origin, destination, VehicleClass.ECONOMY);
                System.out.println(name + ": surge " + t.surgePercent() + "%, next quote "
                        + money(busy.estimateFare(origin, destination, VehicleClass.ECONOMY)));
            } catch (NoDriverAvailableException e) {
                System.out.println(name + ": " + e.getMessage());
            }
            surgeClock.advance(Duration.ofMinutes(1));
        }
        surgeClock.advance(Duration.ofMinutes(11));
        busy.registerDriver(new Driver("D3", "Chen", VehicleClass.ECONOMY));
        busy.goOnline("D3", new Location(1_000, 0));
        System.out.println("after 11 minutes and one new driver: "
                + money(busy.estimateFare(origin, destination, VehicleClass.ECONOMY)));

        System.out.println("== 5. Illegal transitions ==");
        attempt("complete a MATCHED trip", () -> service.complete(
                service.requestRide(ravi, origin, destination, VehicleClass.PREMIUM).id()));
        attempt("rate before completion", () -> service.rateDriver(
                service.requestRide(ravi, origin, destination, VehicleClass.ECONOMY).id(), 5));

        System.out.println("== 6. Eight riders, two drivers, same moment ==");
        RideService rush = new RideService(new NearestDriverStrategy(5_000),
                new MeteredFare(MeteredFare.standardRates()), new DemandSupplySurge(250),
                new MutableClock(Instant.parse("2026-11-02T09:00:00Z")), Duration.ofMinutes(10));
        rush.registerDriver(new Driver("D1", "Asha", VehicleClass.ECONOMY));
        rush.registerDriver(new Driver("D2", "Ben", VehicleClass.ECONOMY));
        rush.goOnline("D1", new Location(500, 0));
        rush.goOnline("D2", new Location(300, 400));
        AtomicInteger matched = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        Set<String> drivers = ConcurrentHashMap.newKeySet();
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Rider rider = new Rider("R" + i, "rider" + i);
            threads.add(new Thread(() -> {
                try {
                    go.await();
                    Trip t = rush.requestRide(rider, origin, destination, VehicleClass.ECONOMY);
                    drivers.add(t.driver().id());
                    matched.incrementAndGet();
                } catch (NoDriverAvailableException e) {
                    refused.incrementAndGet();
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
        System.out.println("matched=" + matched + " refused=" + refused
                + " distinctDrivers=" + drivers.size());
    }

    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": done");
        } catch (RideException | IllegalStateException | IllegalArgumentException e) {
            System.out.println(label + ": refused (" + e.getMessage() + ")");
        }
    }

    static String money(long cents) {
        return String.format("%d.%02d", cents / 100, cents % 100);
    }
}

record Location(int x, int y) {
    /** Manhattan distance in meters on a street grid. */
    long distanceTo(Location other) {
        return Math.abs((long) x - other.x) + Math.abs((long) y - other.y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}

enum VehicleClass { ECONOMY, PREMIUM }

enum DriverStatus { OFFLINE, AVAILABLE, ON_TRIP }

final class RatingBook {
    private int sum;
    private int count;

    static void validate(int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be 1 to 5, got " + stars);
        }
    }

    synchronized void add(int stars) {
        validate(stars);
        sum += stars;
        count++;
    }

    /** Average in hundredths. A user without ratings gets the given default. */
    synchronized int averageHundredths(int defaultValue) {
        return count == 0 ? defaultValue : sum * 100 / count;
    }

    synchronized String describe() {
        return count == 0 ? "none" : String.format("%d.%02d (%d)", sum * 100 / count / 100,
                sum * 100 / count % 100, count);
    }
}

final class Rider {
    private final String id;
    private final String name;
    private final RatingBook ratings = new RatingBook();

    Rider(String id, String name) {
        this.id = id;
        this.name = name;
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    RatingBook ratings() {
        return ratings;
    }
}

final class Driver {
    private final String id;
    private final String name;
    private final VehicleClass vehicleClass;
    private final AtomicReference<DriverStatus> status =
            new AtomicReference<>(DriverStatus.OFFLINE);
    private final RatingBook ratings = new RatingBook();
    private volatile Location location = new Location(0, 0);

    Driver(String id, String name, VehicleClass vehicleClass) {
        this.id = id;
        this.name = name;
        this.vehicleClass = vehicleClass;
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    VehicleClass vehicleClass() {
        return vehicleClass;
    }

    Location location() {
        return location;
    }

    void moveTo(Location newLocation) {
        this.location = newLocation;
    }

    DriverStatus status() {
        return status.get();
    }

    RatingBook ratings() {
        return ratings;
    }

    boolean isAvailable() {
        return status.get() == DriverStatus.AVAILABLE;
    }

    void goOnline(Location where) {
        this.location = where;
        status.compareAndSet(DriverStatus.OFFLINE, DriverStatus.AVAILABLE);
    }

    /** Atomic claim. Exactly one caller wins when several riders pick this driver. */
    boolean tryReserve() {
        return status.compareAndSet(DriverStatus.AVAILABLE, DriverStatus.ON_TRIP);
    }

    void release() {
        status.compareAndSet(DriverStatus.ON_TRIP, DriverStatus.AVAILABLE);
    }
}

record RideRequest(Rider rider, Location pickup, Location dropoff, VehicleClass vehicleClass) {
}

interface MatchingStrategy {
    Optional<Driver> select(RideRequest request, List<Driver> candidates);
}

/** Takes the closest driver inside the pickup radius. Ties go to the lower driver id. */
final class NearestDriverStrategy implements MatchingStrategy {
    private final long maxPickupMeters;

    NearestDriverStrategy(long maxPickupMeters) {
        this.maxPickupMeters = maxPickupMeters;
    }

    @Override
    public Optional<Driver> select(RideRequest request, List<Driver> candidates) {
        Driver best = null;
        long bestDistance = Long.MAX_VALUE;
        for (Driver driver : candidates) {
            long distance = driver.location().distanceTo(request.pickup());
            if (distance > maxPickupMeters) {
                continue;
            }
            if (distance < bestDistance
                    || (distance == bestDistance && driver.id().compareTo(best.id()) < 0)) {
                best = driver;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }
}

/** Takes the best rated driver inside the radius. Ties go to the closer driver. */
final class TopRatedStrategy implements MatchingStrategy {
    private static final int UNRATED_DEFAULT = 400;
    private final long maxPickupMeters;

    TopRatedStrategy(long maxPickupMeters) {
        this.maxPickupMeters = maxPickupMeters;
    }

    @Override
    public Optional<Driver> select(RideRequest request, List<Driver> candidates) {
        Driver best = null;
        for (Driver driver : candidates) {
            if (driver.location().distanceTo(request.pickup()) > maxPickupMeters) {
                continue;
            }
            if (best == null || isBetter(driver, best, request.pickup())) {
                best = driver;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isBetter(Driver a, Driver b, Location pickup) {
        int ratingA = a.ratings().averageHundredths(UNRATED_DEFAULT);
        int ratingB = b.ratings().averageHundredths(UNRATED_DEFAULT);
        if (ratingA != ratingB) {
            return ratingA > ratingB;
        }
        long distanceA = a.location().distanceTo(pickup);
        long distanceB = b.location().distanceTo(pickup);
        if (distanceA != distanceB) {
            return distanceA < distanceB;
        }
        return a.id().compareTo(b.id()) < 0;
    }
}

enum TripState {
    REQUESTED {
        @Override
        TripState onMatch() {
            return MATCHED;
        }

        @Override
        TripState onCancel() {
            return CANCELLED;
        }
    },
    MATCHED {
        @Override
        TripState onArrive() {
            return ARRIVED;
        }

        @Override
        TripState onCancel() {
            return CANCELLED;
        }
    },
    ARRIVED {
        @Override
        TripState onStart() {
            return IN_PROGRESS;
        }

        @Override
        TripState onCancel() {
            return CANCELLED;
        }
    },
    IN_PROGRESS {
        @Override
        TripState onComplete() {
            return COMPLETED;
        }
    },
    COMPLETED,
    CANCELLED;

    TripState onMatch() {
        throw illegal("match");
    }

    TripState onArrive() {
        throw illegal("arrive");
    }

    TripState onStart() {
        throw illegal("start");
    }

    TripState onComplete() {
        throw illegal("complete");
    }

    TripState onCancel() {
        throw illegal("cancel");
    }

    private IllegalStateException illegal(String action) {
        return new IllegalStateException("cannot " + action + " a trip in state " + this);
    }
}

record FareInput(VehicleClass vehicleClass, long meters, long minutes, int surgePercent) {
}

interface FareStrategy {
    long fareCents(FareInput input);
}

record Rate(long baseCents, long perKmCents, long perMinuteCents, long minimumCents) {
}

final class MeteredFare implements FareStrategy {
    private final Map<VehicleClass, Rate> rates;

    MeteredFare(Map<VehicleClass, Rate> rates) {
        this.rates = Map.copyOf(rates);
    }

    static Map<VehicleClass, Rate> standardRates() {
        return Map.of(VehicleClass.ECONOMY, new Rate(250, 120, 30, 500),
                VehicleClass.PREMIUM, new Rate(400, 200, 50, 800));
    }

    @Override
    public long fareCents(FareInput input) {
        Rate rate = rates.get(input.vehicleClass());
        long fare = rate.baseCents() + input.meters() * rate.perKmCents() / 1000
                + input.minutes() * rate.perMinuteCents();
        return Math.max(rate.minimumCents(), fare);
    }
}

/** Decorator: multiplies the fare of another strategy by the surge percent. */
final class SurgeFare implements FareStrategy {
    private final FareStrategy inner;

    SurgeFare(FareStrategy inner) {
        this.inner = inner;
    }

    @Override
    public long fareCents(FareInput input) {
        return (inner.fareCents(input) * input.surgePercent() + 50) / 100;
    }
}

interface SurgePolicy {
    int percent(int recentRequests, int availableDrivers);
}

/** Surge follows the ratio of recent requests to free drivers. 100 means no surge. */
final class DemandSupplySurge implements SurgePolicy {
    private final int capPercent;

    DemandSupplySurge(int capPercent) {
        this.capPercent = capPercent;
    }

    @Override
    public int percent(int recentRequests, int availableDrivers) {
        if (availableDrivers == 0) {
            return capPercent;
        }
        int ratio = 100 * recentRequests / availableDrivers;
        return Math.max(100, Math.min(capPercent, ratio));
    }
}

final class Trip {
    private final String id;
    private final RideRequest request;
    private final int surgePercent;
    private final Instant requestedAt;
    private TripState state = TripState.REQUESTED;
    private Driver driver;
    private Instant startedAt;
    private long fareCents;
    private boolean driverRated;
    private boolean riderRated;

    Trip(String id, RideRequest request, int surgePercent, Instant requestedAt) {
        this.id = id;
        this.request = request;
        this.surgePercent = surgePercent;
        this.requestedAt = requestedAt;
    }

    String id() {
        return id;
    }

    RideRequest request() {
        return request;
    }

    int surgePercent() {
        return surgePercent;
    }

    synchronized TripState state() {
        return state;
    }

    synchronized Driver driver() {
        return driver;
    }

    synchronized long fareCents() {
        return fareCents;
    }

    synchronized void assign(Driver matched) {
        state = state.onMatch();
        driver = matched;
    }

    synchronized void arrive() {
        state = state.onArrive();
    }

    synchronized void start(Instant now) {
        state = state.onStart();
        startedAt = now;
    }

    synchronized void complete(Instant now, FareStrategy fares) {
        state = state.onComplete();
        long seconds = Duration.between(startedAt, now).toSeconds();
        long minutes = (seconds + 59) / 60;
        long meters = request.pickup().distanceTo(request.dropoff());
        fareCents = fares.fareCents(
                new FareInput(request.vehicleClass(), meters, minutes, surgePercent));
    }

    /** Returns the driver that the trip held, or null if no driver was assigned. */
    synchronized Driver cancel() {
        state = state.onCancel();
        return driver;
    }

    synchronized void markDriverRated() {
        requireCompleted();
        if (driverRated) {
            throw new RideException("the driver of " + id + " is already rated");
        }
        driverRated = true;
    }

    synchronized void markRiderRated() {
        requireCompleted();
        if (riderRated) {
            throw new RideException("the rider of " + id + " is already rated");
        }
        riderRated = true;
    }

    private void requireCompleted() {
        if (state != TripState.COMPLETED) {
            throw new RideException("trip " + id + " is " + state + ", not COMPLETED");
        }
    }
}

class RideException extends RuntimeException {
    RideException(String message) {
        super(message);
    }
}

final class NoDriverAvailableException extends RideException {
    NoDriverAvailableException(RideRequest request) {
        super("no " + request.vehicleClass() + " driver near " + request.pickup());
    }
}

final class RideService {
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();
    private final Map<String, Trip> trips = new ConcurrentHashMap<>();
    private final MatchingStrategy matching;
    private final FareStrategy fares;
    private final SurgePolicy surge;
    private final Clock clock;
    private final Duration demandWindow;
    private final Deque<Instant> recentRequests = new ArrayDeque<>();
    private final AtomicLong tripSeq = new AtomicLong();

    RideService(MatchingStrategy matching, FareStrategy fares, SurgePolicy surge,
                Clock clock, Duration demandWindow) {
        this.matching = matching;
        this.fares = fares;
        this.surge = surge;
        this.clock = clock;
        this.demandWindow = demandWindow;
    }

    void registerDriver(Driver driver) {
        drivers.put(driver.id(), driver);
    }

    void goOnline(String driverId, Location where) {
        drivers.get(driverId).goOnline(where);
    }

    List<Driver> availableDrivers(VehicleClass vehicleClass) {
        List<Driver> result = new ArrayList<>();
        for (Driver driver : drivers.values()) {
            if (driver.isAvailable() && driver.vehicleClass() == vehicleClass) {
                result.add(driver);
            }
        }
        return result;
    }

    long estimateFare(Location pickup, Location dropoff, VehicleClass vehicleClass) {
        long meters = pickup.distanceTo(dropoff);
        long minutes = (meters + 499) / 500;
        int percent = surge.percent(demand(false), availableDrivers(vehicleClass).size());
        return fares.fareCents(new FareInput(vehicleClass, meters, minutes, percent));
    }

    Trip requestRide(Rider rider, Location pickup, Location dropoff, VehicleClass vehicleClass) {
        RideRequest request = new RideRequest(rider, pickup, dropoff, vehicleClass);
        int percent = surge.percent(demand(true), availableDrivers(vehicleClass).size());
        Trip trip = new Trip("T-" + tripSeq.incrementAndGet(), request, percent, clock.instant());
        Driver driver = claimDriver(request);
        if (driver == null) {
            trip.cancel();
            throw new NoDriverAvailableException(request);
        }
        trip.assign(driver);
        // Publish the trip only after it holds its driver. A cancel between the claim and
        // the assign would make assign throw and leave the driver stuck in ON_TRIP.
        trips.put(trip.id(), trip);
        return trip;
    }

    private Driver claimDriver(RideRequest request) {
        while (true) {
            Optional<Driver> candidate = matching.select(request,
                    availableDrivers(request.vehicleClass()));
            if (candidate.isEmpty()) {
                return null;
            }
            if (candidate.get().tryReserve()) {
                return candidate.get();
            }
            // Another rider took this driver between select and tryReserve. Select again.
        }
    }

    /** Counts the requests inside the window. The new request counts when record is true. */
    private int demand(boolean record) {
        Instant now = clock.instant();
        synchronized (recentRequests) {
            while (!recentRequests.isEmpty()
                    && recentRequests.peekFirst().isBefore(now.minus(demandWindow))) {
                recentRequests.pollFirst();
            }
            if (record) {
                recentRequests.addLast(now);
                return recentRequests.size();
            }
            return recentRequests.size() + 1;
        }
    }

    void arrive(String tripId) {
        trip(tripId).arrive();
    }

    void start(String tripId) {
        trip(tripId).start(clock.instant());
    }

    void complete(String tripId) {
        Trip trip = trip(tripId);
        trip.complete(clock.instant(), fares);
        Driver driver = trip.driver();
        driver.moveTo(trip.request().dropoff());
        driver.release();
    }

    void cancel(String tripId) {
        Driver driver = trip(tripId).cancel();
        if (driver != null) {
            driver.release();
        }
    }

    void rateDriver(String tripId, int stars) {
        Trip trip = trip(tripId);
        RatingBook.validate(stars);
        trip.markDriverRated();
        trip.driver().ratings().add(stars);
    }

    void rateRider(String tripId, int stars) {
        Trip trip = trip(tripId);
        RatingBook.validate(stars);
        trip.markRiderRated();
        trip.request().rider().ratings().add(stars);
    }

    String describe(Trip trip) {
        String who = trip.driver() == null ? "no driver" : trip.driver().name();
        String text = trip.id() + " " + trip.state() + " " + who + " surge "
                + trip.surgePercent() + "%";
        if (trip.state() == TripState.COMPLETED) {
            text += " fare " + Main.money(trip.fareCents());
        }
        return text;
    }

    private Trip trip(String tripId) {
        Trip trip = trips.get(tripId);
        if (trip == null) {
            throw new RideException("unknown trip " + tripId);
        }
        return trip;
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
