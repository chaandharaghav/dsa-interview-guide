import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        Display display = System.out::println;

        System.out.println("== Machine 1: normal sales ==");
        VendingMachine m1 = new VendingMachine(display);
        m1.addProduct(new Product("A1", "Cola", 150), 2);
        m1.addProduct(new Product("A2", "Chips", 100), 1);
        m1.addProduct(new Product("B1", "Candy", 65), 5);
        m1.addProduct(new Product("B2", "Gum", 35), 5);
        m1.loadChange(Coin.NICKEL, 1);
        m1.loadChange(Coin.DIME, 1);
        m1.loadChange(Coin.QUARTER, 2);
        System.out.println("State: " + m1.stateName() + ", change in bank: " + Cents.format(
                m1.bankTotal()));

        System.out.println("-- Select with no money");
        attempt(() -> m1.selectProduct("A1"));

        System.out.println("-- Pay for Candy in several steps, no change");
        m1.insertCoin(Coin.QUARTER);
        m1.insertCoin(Coin.QUARTER);
        System.out.println("State: " + m1.stateName());
        m1.selectProduct("B1");
        m1.insertCoin(Coin.DIME);
        m1.insertCoin(Coin.NICKEL);
        m1.selectProduct("B1");
        System.out.println("State: " + m1.stateName());

        System.out.println("-- Refund");
        m1.insertCoin(Coin.QUARTER);
        m1.insertCoin(Coin.DIME);
        m1.refund();
        System.out.println("State: " + m1.stateName());

        System.out.println("-- Overpay, machine makes change");
        m1.insertCoin(Coin.DOLLAR);
        m1.selectProduct("B2");

        System.out.println("-- Unknown product, then wrong action in a state");
        m1.insertCoin(Coin.DOLLAR);
        m1.selectProduct("Z9");
        attempt(() -> m1.dispense());
        m1.refund();

        System.out.println("-- Last Chips sold, then a second Chips request");
        m1.insertCoin(Coin.DOLLAR);
        m1.selectProduct("A2");
        m1.insertCoin(Coin.DOLLAR);
        m1.selectProduct("A2");
        m1.refund();
        System.out.println("Stock: " + m1.stockReport());

        System.out.println();
        System.out.println("== Machine 2: empty coin bank, one product ==");
        VendingMachine m2 = new VendingMachine(display);
        m2.addProduct(new Product("C1", "Gum", 35), 1);
        System.out.println("-- Overpay with no change available");
        m2.insertCoin(Coin.DOLLAR);
        m2.selectProduct("C1");
        System.out.println("State: " + m2.stateName());

        System.out.println("-- Exact payment sells the last item");
        m2.insertCoin(Coin.QUARTER);
        m2.insertCoin(Coin.DIME);
        m2.selectProduct("C1");
        System.out.println("State: " + m2.stateName());

        System.out.println("-- Sold out machine rejects coins, restock reopens it");
        m2.insertCoin(Coin.QUARTER);
        m2.restock("C1", 3);
        System.out.println("State: " + m2.stateName());
        System.out.println("Stock: " + m2.stockReport());
    }

    private static void attempt(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

enum Coin {
    NICKEL(5), DIME(10), QUARTER(25), DOLLAR(100);

    private final int cents;

    Coin(int cents) {
        this.cents = cents;
    }

    int cents() {
        return cents;
    }
}

final class Cents {
    private Cents() { }

    static String format(int cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }

    static String describe(Map<Coin, Integer> coins) {
        return coins.entrySet().stream()
                .map(e -> e.getKey() + " x" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    static int total(Map<Coin, Integer> coins) {
        int sum = 0;
        for (Map.Entry<Coin, Integer> e : coins.entrySet()) {
            sum += e.getKey().cents() * e.getValue();
        }
        return sum;
    }
}

record Product(String code, String name, int price) {
    Product {
        if (price <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
    }
}

interface Display {
    void show(String message);
}

class Inventory {
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Integer> quantities = new LinkedHashMap<>();

    void add(Product product, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        products.putIfAbsent(product.code(), product);
        quantities.merge(product.code(), quantity, Integer::sum);
    }

    Optional<Product> find(String code) {
        return Optional.ofNullable(products.get(code));
    }

    boolean inStock(String code) {
        return quantities.getOrDefault(code, 0) > 0;
    }

    void take(String code) {
        if (!inStock(code)) {
            throw new IllegalStateException("Product " + code + " is sold out");
        }
        quantities.merge(code, -1, Integer::sum);
    }

    boolean allSoldOut() {
        return quantities.values().stream().allMatch(q -> q == 0);
    }

    String report() {
        return quantities.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}

class CoinBank {
    private static final Coin[] DESCENDING = {Coin.DOLLAR, Coin.QUARTER, Coin.DIME, Coin.NICKEL};
    private final Map<Coin, Integer> counts = new EnumMap<>(Coin.class);

    void deposit(Coin coin, int count) {
        counts.merge(coin, count, Integer::sum);
    }

    void depositAll(Map<Coin, Integer> coins) {
        coins.forEach(this::deposit);
    }

    void withdrawAll(Map<Coin, Integer> coins) {
        coins.forEach((coin, count) -> {
            if (counts.getOrDefault(coin, 0) < count) {
                throw new IllegalStateException("Bank has too few " + coin);
            }
            counts.merge(coin, -count, Integer::sum);
        });
    }

    int total() {
        return Cents.total(counts);
    }

    // Finds coins that add up to the amount, or returns empty. Does not change the bank.
    Optional<Map<Coin, Integer>> makeChange(int amount) {
        return search(0, amount, new EnumMap<>(Coin.class));
    }

    private Optional<Map<Coin, Integer>> search(int index, int remaining,
                                                Map<Coin, Integer> chosen) {
        if (remaining == 0) {
            return Optional.of(new EnumMap<>(chosen));
        }
        if (index == DESCENDING.length) {
            return Optional.empty();
        }
        Coin coin = DESCENDING[index];
        int most = Math.min(counts.getOrDefault(coin, 0), remaining / coin.cents());
        for (int use = most; use >= 0; use--) {
            if (use > 0) {
                chosen.put(coin, use);
            } else {
                chosen.remove(coin);
            }
            Optional<Map<Coin, Integer>> found = search(index + 1, remaining - use * coin.cents(),
                    chosen);
            if (found.isPresent()) {
                return found;
            }
        }
        chosen.remove(coin);
        return Optional.empty();
    }
}

interface VendingState {
    String name();

    default void insertCoin(VendingMachine machine, Coin coin) {
        throw new IllegalStateException("Cannot insert a coin in state " + name());
    }

    default void selectProduct(VendingMachine machine, String code) {
        throw new IllegalStateException("Cannot select a product in state " + name());
    }

    default void refund(VendingMachine machine) {
        throw new IllegalStateException("Cannot refund in state " + name());
    }

    default void dispense(VendingMachine machine) {
        throw new IllegalStateException("Cannot dispense in state " + name());
    }
}

class IdleState implements VendingState {
    @Override
    public String name() {
        return "Idle";
    }

    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        machine.acceptCoin(coin);
        machine.setState(machine.hasMoneyState());
    }

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        machine.display().show("Insert coins first");
    }
}

class HasMoneyState implements VendingState {
    @Override
    public String name() {
        return "HasMoney";
    }

    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        machine.acceptCoin(coin);
    }

    @Override
    public void selectProduct(VendingMachine machine, String code) {
        Optional<Product> product = machine.inventory().find(code);
        if (product.isEmpty()) {
            machine.display().show("Unknown product " + code);
        } else if (!machine.inventory().inStock(code)) {
            machine.display().show("Sold out: " + product.get().name());
        } else if (machine.balance() < product.get().price()) {
            int missing = product.get().price() - machine.balance();
            machine.display().show("Insert " + Cents.format(missing) + " more for "
                    + product.get().name());
        } else {
            machine.select(product.get());
            machine.setState(machine.dispensingState());
            machine.dispense();
        }
    }

    @Override
    public void refund(VendingMachine machine) {
        machine.refundPending();
        machine.setState(machine.idleState());
    }
}

class DispensingState implements VendingState {
    @Override
    public String name() {
        return "Dispensing";
    }

    @Override
    public void dispense(VendingMachine machine) {
        Product product = machine.selected();
        int changeDue = machine.balance() - product.price();
        machine.bank().depositAll(machine.pending());
        Optional<Map<Coin, Integer>> change = machine.bank().makeChange(changeDue);
        if (change.isEmpty()) {
            machine.bank().withdrawAll(machine.pending());
            machine.display().show("Exact change only. Cannot give " + Cents.format(changeDue)
                    + " change");
            machine.refundPending();
        } else {
            machine.bank().withdrawAll(change.get());
            machine.inventory().take(product.code());
            String changeText = changeDue == 0 ? "no change"
                    : "change " + Cents.format(changeDue) + " (" + Cents.describe(change.get())
                    + ")";
            machine.display().show("Dispensed " + product.name() + ", " + changeText);
            machine.clearPending();
        }
        machine.setState(machine.inventory().allSoldOut()
                ? machine.soldOutState() : machine.idleState());
    }
}

class SoldOutState implements VendingState {
    @Override
    public String name() {
        return "SoldOut";
    }

    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        machine.display().show("Sold out. Returned " + coin);
    }
}

class VendingMachine {
    private final Display display;
    private final Inventory inventory = new Inventory();
    private final CoinBank bank = new CoinBank();
    private final Map<Coin, Integer> pending = new EnumMap<>(Coin.class);
    private final VendingState idle = new IdleState();
    private final VendingState hasMoney = new HasMoneyState();
    private final VendingState dispensing = new DispensingState();
    private final VendingState soldOut = new SoldOutState();
    private VendingState state = soldOut;
    private Product selected;

    VendingMachine(Display display) {
        this.display = display;
    }

    // Public operations: each one holds the machine lock and delegates to the current state.
    public synchronized void insertCoin(Coin coin) {
        state.insertCoin(this, coin);
    }

    public synchronized void selectProduct(String code) {
        state.selectProduct(this, code);
    }

    public synchronized void refund() {
        state.refund(this);
    }

    public synchronized void dispense() {
        state.dispense(this);
    }

    // Operator operations.
    public synchronized void addProduct(Product product, int quantity) {
        inventory.add(product, quantity);
        if (quantity > 0 && state == soldOut) {
            state = idle;
        }
    }

    public synchronized void restock(String code, int quantity) {
        Product product = inventory.find(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product " + code));
        addProduct(product, quantity);
    }

    public synchronized void loadChange(Coin coin, int count) {
        bank.deposit(coin, count);
    }

    public synchronized int bankTotal() {
        return bank.total();
    }

    public synchronized String stateName() {
        return state.name();
    }

    public synchronized String stockReport() {
        return inventory.report();
    }

    // Hooks for the state classes.
    void acceptCoin(Coin coin) {
        pending.merge(coin, 1, Integer::sum);
        display.show("Inserted " + coin + ", balance " + Cents.format(balance()));
    }

    int balance() {
        return Cents.total(pending);
    }

    void refundPending() {
        display.show("Refunded " + Cents.format(balance()) + " (" + Cents.describe(pending) + ")");
        clearPending();
    }

    void clearPending() {
        pending.clear();
        selected = null;
    }

    void select(Product product) {
        selected = product;
    }

    Product selected() {
        return selected;
    }

    Map<Coin, Integer> pending() {
        return pending;
    }

    Inventory inventory() {
        return inventory;
    }

    CoinBank bank() {
        return bank;
    }

    Display display() {
        return display;
    }

    void setState(VendingState next) {
        state = next;
    }

    VendingState idleState() {
        return idle;
    }

    VendingState hasMoneyState() {
        return hasMoney;
    }

    VendingState dispensingState() {
        return dispensing;
    }

    VendingState soldOutState() {
        return soldOut;
    }
}
