import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        demoHallCallAndCarCall();
        demoDispatchStrategies();
        demoStopPolicies();
        demoConcurrentButtons();
        demoValidation();
    }

    private static void demoHallCallAndCarCall() {
        System.out.println("== Two cars, 10 floors: a hall call, then a car call ==");
        ElevatorSystem system = new ElevatorSystem(
                9, List.of(0, 9), new NearestCarStrategy(), new LookPolicy());
        int car = system.submit(new HallCall(3, Direction.UP));
        System.out.println("hall call at floor 3 going UP -> car " + car);
        system.tick(3);
        System.out.println(system.status());
        system.submit(new CarCall(car, 7));
        System.out.println("passenger in car " + car + " presses 7");
        while (!system.isIdle()) {
            system.tick(1);
            System.out.println(system.status());
        }
    }

    private static void demoDispatchStrategies() {
        System.out.println("== Dispatch: car 0 heads UP to 9 (floor 3), car 1 idle at 4 ==");
        for (DispatchStrategy strategy
                : List.of(new NearestCarStrategy(), new DirectionAwareStrategy())) {
            ElevatorSystem system =
                    new ElevatorSystem(9, List.of(2, 4), strategy, new LookPolicy());
            system.submit(new CarCall(0, 9));
            system.tick(1);
            int chosen = system.submit(new HallCall(1, Direction.UP));
            System.out.println(strategy.getClass().getSimpleName()
                    + " sends the hall call at floor 1 to car " + chosen);
        }
    }

    private static void demoStopPolicies() {
        System.out.println("== Stop order: car at floor 5, stops 6, 4, 9, 2 ==");
        for (StopPolicy policy : List.of(new LookPolicy(), new NearestStopPolicy())) {
            ElevatorSystem system =
                    new ElevatorSystem(9, List.of(5), new NearestCarStrategy(), policy);
            for (int floor : new int[] {6, 4, 9, 2}) {
                system.submit(new CarCall(0, floor));
            }
            int ticks = 0;
            while (!system.isIdle()) {
                system.tick(1);
                ticks++;
            }
            System.out.println(policy.getClass().getSimpleName() + ": visited "
                    + system.car(0).servedFloors() + " in " + ticks + " ticks");
        }
    }

    private static void demoConcurrentButtons() throws InterruptedException {
        System.out.println("== Concurrency: 8 threads press floors 1..49 in one car ==");
        ElevatorSystem system = new ElevatorSystem(
                49, List.of(0), new NearestCarStrategy(), new LookPolicy());
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            Thread thread = new Thread(() -> {
                for (int floor = 1; floor <= 49; floor++) {
                    system.submit(new CarCall(0, floor));
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("pending stops: " + system.car(0).pendingStops());
        while (!system.isIdle()) {
            system.tick(1);
        }
        System.out.println("floors served: " + system.car(0).servedFloors().size());
    }

    private static void demoValidation() {
        System.out.println("== Validation ==");
        ElevatorSystem system = new ElevatorSystem(
                9, List.of(0), new NearestCarStrategy(), new LookPolicy());
        try {
            system.submit(new CarCall(0, 12));
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }
        try {
            system.submit(new HallCall(9, Direction.UP));
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }
        try {
            system.submit(new CarCall(5, 3));
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }
    }
}

enum Direction {
    UP, DOWN, IDLE
}

sealed interface Request permits HallCall, CarCall {
}

record HallCall(int floor, Direction direction) implements Request {
    HallCall {
        Objects.requireNonNull(direction, "direction");
        if (direction == Direction.IDLE) {
            throw new IllegalArgumentException("a hall call needs UP or DOWN");
        }
    }
}

record CarCall(int carId, int floor) implements Request {
}

record CarView(int id, int floor, Direction direction, int farthestStop) {
}

interface DispatchStrategy {
    int selectCar(List<CarView> cars, HallCall call);
}

final class NearestCarStrategy implements DispatchStrategy {
    @Override
    public int selectCar(List<CarView> cars, HallCall call) {
        CarView best = cars.get(0);
        for (CarView car : cars) {
            if (Math.abs(car.floor() - call.floor()) < Math.abs(best.floor() - call.floor())) {
                best = car;
            }
        }
        return best.id();
    }
}

final class DirectionAwareStrategy implements DispatchStrategy {
    @Override
    public int selectCar(List<CarView> cars, HallCall call) {
        CarView best = cars.get(0);
        for (CarView car : cars) {
            if (cost(car, call) < cost(best, call)) {
                best = car;
            }
        }
        return best.id();
    }

    private int cost(CarView car, HallCall call) {
        int distance = Math.abs(car.floor() - call.floor());
        if (car.direction() == Direction.IDLE) {
            return distance;
        }
        boolean ahead = car.direction() == Direction.UP
                ? call.floor() >= car.floor()
                : call.floor() <= car.floor();
        if (ahead && call.direction() == car.direction()) {
            return distance;
        }
        return Math.abs(car.farthestStop() - car.floor())
                + Math.abs(car.farthestStop() - call.floor());
    }
}

interface StopPolicy {
    Direction choose(int floor, Direction heading, NavigableSet<Integer> stops);
}

final class LookPolicy implements StopPolicy {
    @Override
    public Direction choose(int floor, Direction heading, NavigableSet<Integer> stops) {
        Integer above = stops.higher(floor);
        Integer below = stops.lower(floor);
        if (heading == Direction.UP && above != null) {
            return Direction.UP;
        }
        if (heading == Direction.DOWN && below != null) {
            return Direction.DOWN;
        }
        if (above != null && below != null) {
            return floor - below < above - floor ? Direction.DOWN : Direction.UP;
        }
        return above != null ? Direction.UP : below != null ? Direction.DOWN : Direction.IDLE;
    }
}

final class NearestStopPolicy implements StopPolicy {
    @Override
    public Direction choose(int floor, Direction heading, NavigableSet<Integer> stops) {
        Integer above = stops.higher(floor);
        Integer below = stops.lower(floor);
        if (above != null && below != null) {
            return floor - below < above - floor ? Direction.DOWN : Direction.UP;
        }
        return above != null ? Direction.UP : below != null ? Direction.DOWN : Direction.IDLE;
    }
}

interface CarState {
    String label();

    void step(ElevatorCar car);
}

final class IdleState implements CarState {
    @Override
    public String label() {
        return "IDLE";
    }

    @Override
    public void step(ElevatorCar car) {
        if (car.hasStopHere()) {
            car.openDoor();
            return;
        }
        Direction direction = car.chooseDirection();
        if (direction != Direction.IDLE) {
            car.setState(new MovingState(direction));
            car.currentState().step(car);
        }
    }
}

final class MovingState implements CarState {
    private final Direction direction;

    MovingState(Direction direction) {
        this.direction = direction;
    }

    @Override
    public String label() {
        return "MOVING_" + direction;
    }

    @Override
    public void step(ElevatorCar car) {
        car.moveOneFloor(direction);
        if (car.hasStopHere()) {
            car.openDoor();
        }
    }
}

final class DoorOpenState implements CarState {
    @Override
    public String label() {
        return "DOOR_OPEN";
    }

    @Override
    public void step(ElevatorCar car) {
        car.setState(new IdleState());
    }
}

final class ElevatorCar {
    private final int id;
    private final int topFloor;
    private final StopPolicy policy;
    private final TreeSet<Integer> stops = new TreeSet<>();
    private final List<Integer> served = new ArrayList<>();
    private int floor;
    private Direction heading = Direction.IDLE;
    private CarState state = new IdleState();

    ElevatorCar(int id, int startFloor, int topFloor, StopPolicy policy) {
        if (startFloor < 0 || startFloor > topFloor) {
            throw new IllegalArgumentException("start floor " + startFloor + " is outside 0.."
                    + topFloor);
        }
        this.id = id;
        this.floor = startFloor;
        this.topFloor = topFloor;
        this.policy = policy;
    }

    synchronized void addStop(int target) {
        if (target < 0 || target > topFloor) {
            throw new IllegalArgumentException("floor " + target + " is outside 0.." + topFloor);
        }
        if (target == floor && state instanceof DoorOpenState) {
            return;
        }
        stops.add(target);
    }

    synchronized void step() {
        state.step(this);
    }

    // A moving car keeps its direction. A stopped car reports the direction that the
    // policy will choose next, so a heading left over from the last trip does not count.
    synchronized CarView view() {
        Direction next = state instanceof MovingState ? heading
                : policy.choose(floor, heading, stops);
        int farthest = next == Direction.UP ? stops.last()
                : next == Direction.DOWN ? stops.first()
                : floor;
        return new CarView(id, floor, next, farthest);
    }

    synchronized int pendingStops() {
        return stops.size();
    }

    synchronized List<Integer> servedFloors() {
        return List.copyOf(served);
    }

    synchronized boolean isIdle() {
        return state instanceof IdleState && stops.isEmpty();
    }

    synchronized String describe() {
        return "car " + id + " floor " + floor + " " + state.label();
    }

    // The methods below run inside step(), which holds the lock of the car.
    boolean hasStopHere() {
        return stops.contains(floor);
    }

    Direction chooseDirection() {
        heading = policy.choose(floor, heading, stops);
        return heading;
    }

    void moveOneFloor(Direction direction) {
        int next = floor + (direction == Direction.UP ? 1 : -1);
        if (next < 0 || next > topFloor) {
            throw new IllegalStateException("car " + id + " cannot move to floor " + next);
        }
        floor = next;
    }

    void openDoor() {
        stops.remove(floor);
        served.add(floor);
        state = new DoorOpenState();
    }

    void setState(CarState next) {
        state = next;
    }

    CarState currentState() {
        return state;
    }
}

final class ElevatorSystem {
    private final int topFloor;
    private final List<ElevatorCar> cars = new ArrayList<>();
    private final DispatchStrategy dispatch;
    private int tickCount;

    ElevatorSystem(int topFloor, List<Integer> startFloors,
                   DispatchStrategy dispatch, StopPolicy policy) {
        if (topFloor < 1 || startFloors.isEmpty()) {
            throw new IllegalArgumentException("need at least 2 floors and 1 car");
        }
        this.topFloor = topFloor;
        this.dispatch = dispatch;
        for (int i = 0; i < startFloors.size(); i++) {
            cars.add(new ElevatorCar(i, startFloors.get(i), topFloor, policy));
        }
    }

    int submit(Request request) {
        return switch (request) {
            case HallCall call -> {
                if (call.floor() < 0 || call.floor() > topFloor) {
                    throw new IllegalArgumentException("floor " + call.floor() + " is outside 0.."
                            + topFloor);
                }
                if (call.floor() == topFloor && call.direction() == Direction.UP) {
                    throw new IllegalArgumentException("no UP call at the top floor");
                }
                if (call.floor() == 0 && call.direction() == Direction.DOWN) {
                    throw new IllegalArgumentException("no DOWN call at the ground floor");
                }
                List<CarView> views = new ArrayList<>();
                for (ElevatorCar car : cars) {
                    views.add(car.view());
                }
                int carId = dispatch.selectCar(views, call);
                cars.get(carId).addStop(call.floor());
                yield carId;
            }
            case CarCall call -> {
                if (call.carId() < 0 || call.carId() >= cars.size()) {
                    throw new IllegalArgumentException("no car " + call.carId());
                }
                cars.get(call.carId()).addStop(call.floor());
                yield call.carId();
            }
        };
    }

    void tick(int steps) {
        for (int i = 0; i < steps; i++) {
            tickCount++;
            for (ElevatorCar car : cars) {
                car.step();
            }
        }
    }

    boolean isIdle() {
        return cars.stream().allMatch(ElevatorCar::isIdle);
    }

    ElevatorCar car(int id) {
        return cars.get(id);
    }

    String status() {
        StringBuilder text = new StringBuilder("t=" + tickCount);
        for (ElevatorCar car : cars) {
            text.append(" | ").append(car.describe());
        }
        return text.toString();
    }
}
