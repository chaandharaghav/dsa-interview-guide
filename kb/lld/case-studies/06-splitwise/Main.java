import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ExpenseService service = new ExpenseService();
        for (String name : List.of("alice", "bob", "carol", "dave")) {
            service.addUser(name);
        }
        service.createGroup("goa", "Goa Trip", List.of("alice", "bob", "carol", "dave"));

        System.out.println("== Expenses ==");
        step("Dinner, equal", () -> service.addExpense("goa", "Dinner", "alice", new Money(10_000),
                new EqualSplit(List.of("alice", "bob", "carol"))).describe());
        step("Cab, exact", () -> service.addExpense("goa", "Cab", "bob", new Money(4_500),
                new ExactSplit(Map.of("alice", new Money(1_000), "bob", new Money(1_500),
                        "carol", new Money(2_000)))).describe());
        step("Hotel, percent", () -> service.addExpense("goa", "Hotel", "carol", new Money(60_000),
                new PercentSplit(Map.of("alice", 4_000, "bob", 3_000, "carol", 2_000,
                        "dave", 1_000))).describe());
        step("Snacks, equal", () -> service.addExpense("goa", "Snacks", "dave", new Money(9_001),
                new EqualSplit(List.of("alice", "bob", "dave"))).describe());
        step("Tickets, percent", () -> service.addExpense("goa", "Tickets", "alice",
                new Money(10_001), new PercentSplit(Map.of("bob", 3_333, "carol", 3_333,
                        "dave", 3_334))).describe());

        System.out.println("== Rejected input ==");
        step("Exact amounts too small", () -> service.addExpense("goa", "Fuel", "bob",
                new Money(10_000), new ExactSplit(Map.of("alice", new Money(5_000),
                        "bob", new Money(4_900)))).describe());
        step("Percent below 100", () -> service.addExpense("goa", "Fuel", "bob",
                new Money(10_000), new PercentSplit(Map.of("alice", 5_000, "bob", 4_999)))
                .describe());
        step("Payer outside the group", () -> service.addExpense("goa", "Fuel", "mallory",
                new Money(10_000), new EqualSplit(List.of("alice", "bob"))).describe());

        System.out.println("== Balances ==");
        System.out.println("Who owes whom:");
        List<Debt> pairwise = service.debts("goa");
        pairwise.forEach(debt -> System.out.println("  " + debt.describe()));
        System.out.println("Net balances:");
        service.balances("goa").forEach((user, minor) ->
                System.out.println("  " + user + " " + Money.signed(minor)));

        System.out.println("== Simplify ==");
        List<Debt> simplified = service.simplify("goa");
        System.out.println(pairwise.size() + " pairwise debts become "
                + simplified.size() + " transfers:");
        simplified.forEach(debt -> System.out.println("  " + debt.describe()));
        for (Debt debt : simplified) {
            service.settle("goa", debt.from(), debt.to(), debt.amount());
        }
        boolean allZero = service.balances("goa").values().stream().allMatch(v -> v == 0);
        System.out.println("Every net balance is zero after the transfers: " + allZero);
        List<Debt> left = service.debts("goa");
        System.out.println("Pairwise debts left, because simplify did not rewrite them: "
                + left.size());
        left.forEach(debt -> System.out.println("  " + debt.describe()));

        Map<String, Long> hard = Map.of("A", 600L, "B", 500L, "C", -500L, "D", -400L, "E", -200L);
        System.out.println("Greedy on A+6 B+5 C-5 D-4 E-2: "
                + DebtSimplifier.simplify(hard).size() + " transfers");

        System.out.println("== Concurrency ==");
        service.createGroup("busy", "Busy Group", List.of("alice", "bob", "carol", "dave"));
        List<String> members = List.of("alice", "bob", "carol", "dave");
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            String payer = members.get(t % 4);
            Money amount = new Money(1_000L * (t + 1));
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 250; i++) {
                    service.addExpense("busy", "Shared cost", payer, amount,
                            new EqualSplit(members));
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("Expenses recorded: " + service.expenseCount("busy"));
        Map<String, Long> net = service.balances("busy");
        net.forEach((user, minor) -> System.out.println("  " + user + " " + Money.signed(minor)));
        System.out.println("Net balances add up to: "
                + Money.signed(net.values().stream().mapToLong(Long::longValue).sum()));
    }

    static void step(String label, Supplier<String> action) {
        try {
            System.out.println(label + ": " + action.get());
        } catch (IllegalArgumentException e) {
            System.out.println(label + " -> rejected: " + e.getMessage());
        }
    }
}

// ---------- Value objects ----------

record Money(long minor) {
    Money {
        if (minor < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    // Signed text for a balance: positive means "is owed", negative means "owes".
    static String signed(long minor) {
        String sign = minor < 0 ? "-" : minor > 0 ? "+" : "";
        long abs = Math.abs(minor);
        return String.format(Locale.ROOT, "%s%d.%02d", sign, abs / 100, abs % 100);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d.%02d", minor / 100, minor % 100);
    }
}

record User(String id) { }

record Debt(String from, String to, Money amount) {
    String describe() {
        return from + " owes " + to + " " + amount;
    }
}

// ---------- Split strategies ----------

interface SplitStrategy {
    // Returns the share of each participant. The shares add up to the total exactly.
    Map<String, Money> shares(Money total);
}

record EqualSplit(List<String> participants) implements SplitStrategy {
    EqualSplit {
        if (participants.isEmpty()
                || participants.stream().distinct().count() != participants.size()) {
            throw new IllegalArgumentException("Equal split needs different participants");
        }
        participants = List.copyOf(participants);
    }

    // The remainder (total mod n) goes to the first participants, one minor unit each.
    @Override
    public Map<String, Money> shares(Money total) {
        int count = participants.size();
        long each = total.minor() / count;
        long extra = total.minor() % count;
        Map<String, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(participants.get(i), new Money(each + (i < extra ? 1 : 0)));
        }
        return result;
    }
}

record ExactSplit(Map<String, Money> amounts) implements SplitStrategy {
    ExactSplit {
        if (amounts.isEmpty()) {
            throw new IllegalArgumentException("Exact split needs participants");
        }
        amounts = Map.copyOf(amounts);
    }

    @Override
    public Map<String, Money> shares(Money total) {
        long sum = amounts.values().stream().mapToLong(Money::minor).sum();
        if (sum != total.minor()) {
            throw new IllegalArgumentException("Exact amounts add up to " + new Money(sum)
                    + " but the expense is " + total);
        }
        return new TreeMap<>(amounts);
    }
}

// Percentages are whole basis points: 3333 means 33.33 percent. All must add up to 10000.
record PercentSplit(Map<String, Integer> basisPoints) implements SplitStrategy {
    private static final int WHOLE = 10_000;

    PercentSplit {
        if (basisPoints.isEmpty() || basisPoints.values().stream().anyMatch(bp -> bp < 0)) {
            throw new IllegalArgumentException("Percent split needs non-negative percentages");
        }
        int sum = basisPoints.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != WHOLE) {
            throw new IllegalArgumentException("Percentages add up to "
                    + String.format(Locale.ROOT, "%d.%02d", sum / 100, sum % 100)
                    + ", not 100.00");
        }
        basisPoints = Map.copyOf(basisPoints);
    }

    // Largest remainder method: round every share down, then give the missing minor
    // units to the shares that lost the largest fraction. The shares add up exactly.
    @Override
    public Map<String, Money> shares(Money total) {
        List<String> users = basisPoints.keySet().stream().sorted().toList();
        int count = users.size();
        long[] shares = new long[count];
        long[] remainders = new long[count];
        long assigned = 0;
        for (int i = 0; i < count; i++) {
            long product = Math.multiplyExact(total.minor(), (long) basisPoints.get(users.get(i)));
            shares[i] = product / WHOLE;
            remainders[i] = product % WHOLE;
            assigned += shares[i];
        }
        long missing = total.minor() - assigned;
        List<Integer> order = IntStream.range(0, count).boxed()
                .sorted(Comparator.comparingLong((Integer i) -> -remainders[i])
                        .thenComparingInt(i -> i))
                .toList();
        for (int k = 0; k < missing; k++) {
            shares[order.get(k)]++;
        }
        Map<String, Money> result = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            result.put(users.get(i), new Money(shares[i]));
        }
        return result;
    }
}

// ---------- Expense: immutable, and valid by construction ----------

record Expense(String id, String description, String paidBy, Money total,
               Map<String, Money> shares) {
    Expense {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (total.minor() <= 0) {
            throw new IllegalArgumentException("Expense must be positive");
        }
        long sum = shares.values().stream().mapToLong(Money::minor).sum();
        if (shares.isEmpty() || sum != total.minor()) {
            throw new IllegalArgumentException("Shares must add up to the total");
        }
        shares = Map.copyOf(shares);
    }

    String describe() {
        String text = new TreeMap<>(shares).entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
        return id + " " + total + " paid by " + paidBy + ": " + text;
    }
}

// ---------- Ledger: who owes whom, one number for each pair of users ----------

final class Ledger {
    private record Pair(String low, String high) { }

    // The value is what "low" owes "high". A negative value means "high" owes "low".
    private final Map<Pair, Long> lowOwesHigh = new HashMap<>();

    void owe(String debtor, String creditor, long minor) {
        if (debtor.compareTo(creditor) < 0) {
            lowOwesHigh.merge(new Pair(debtor, creditor), minor, Long::sum);
        } else {
            lowOwesHigh.merge(new Pair(creditor, debtor), -minor, Long::sum);
        }
    }

    List<Debt> debts() {
        List<Debt> result = new ArrayList<>();
        lowOwesHigh.forEach((pair, value) -> {
            if (value > 0) {
                result.add(new Debt(pair.low(), pair.high(), new Money(value)));
            } else if (value < 0) {
                result.add(new Debt(pair.high(), pair.low(), new Money(-value)));
            }
        });
        result.sort(Comparator.comparing(Debt::from).thenComparing(Debt::to));
        return result;
    }

    // Positive: other users owe this user. Negative: this user owes others.
    Map<String, Long> netBalances(Collection<String> members) {
        Map<String, Long> net = new TreeMap<>();
        members.forEach(member -> net.put(member, 0L));
        lowOwesHigh.forEach((pair, value) -> {
            net.merge(pair.low(), -value, Long::sum);
            net.merge(pair.high(), value, Long::sum);
        });
        return net;
    }
}

// ---------- Group: the unit of locking ----------

final class Group {
    private final String id;
    private final String name;
    private final Set<String> members;
    private final Ledger ledger = new Ledger();
    private final List<Expense> expenses = new ArrayList<>();

    Group(String id, String name, Collection<String> members) {
        this.id = id;
        this.name = name;
        this.members = new LinkedHashSet<>(members);
    }

    synchronized void addExpense(Expense expense) {
        requireMember(expense.paidBy());
        expense.shares().keySet().forEach(this::requireMember);
        expenses.add(expense);
        expense.shares().forEach((user, share) -> {
            if (!user.equals(expense.paidBy())) {
                ledger.owe(user, expense.paidBy(), share.minor());
            }
        });
    }

    // "from" pays "to" to reduce what "from" owes. The ledger sees it as a debt in reverse.
    synchronized void settle(String from, String to, Money amount) {
        requireMember(from);
        requireMember(to);
        if (amount.minor() <= 0 || from.equals(to)) {
            throw new IllegalArgumentException("Settlement needs two users and a positive amount");
        }
        ledger.owe(to, from, amount.minor());
    }

    synchronized List<Debt> debts() {
        return ledger.debts();
    }

    synchronized Map<String, Long> netBalances() {
        return ledger.netBalances(members);
    }

    synchronized int expenseCount() {
        return expenses.size();
    }

    private void requireMember(String userId) {
        if (!members.contains(userId)) {
            throw new IllegalArgumentException(userId + " is not a member of " + name);
        }
    }
}

// ---------- Debt simplification: greedy, with two heaps ----------

final class DebtSimplifier {
    private record Balance(String user, long amount) { }

    // Largest amount first. The user id breaks ties, so the result is deterministic.
    private static final Comparator<Balance> LARGEST_FIRST =
            Comparator.comparingLong(Balance::amount).reversed().thenComparing(Balance::user);

    // net: positive means the user is owed money, negative means the user owes money.
    // Time O(n log n), space O(n), at most n - 1 transfers for n users with a balance.
    static List<Debt> simplify(Map<String, Long> net) {
        if (net.values().stream().mapToLong(Long::longValue).sum() != 0) {
            throw new IllegalArgumentException("Net balances must add up to zero");
        }
        PriorityQueue<Balance> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Balance> debtors = new PriorityQueue<>(LARGEST_FIRST);
        net.forEach((user, amount) -> {
            if (amount > 0) {
                creditors.add(new Balance(user, amount));
            } else if (amount < 0) {
                debtors.add(new Balance(user, -amount));
            }
        });
        List<Debt> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();
            long moved = Math.min(creditor.amount(), debtor.amount());
            transfers.add(new Debt(debtor.user(), creditor.user(), new Money(moved)));
            if (creditor.amount() > moved) {
                creditors.add(new Balance(creditor.user(), creditor.amount() - moved));
            }
            if (debtor.amount() > moved) {
                debtors.add(new Balance(debtor.user(), debtor.amount() - moved));
            }
        }
        return transfers;
    }
}

// ---------- Facade ----------

final class ExpenseService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Group> groups = new ConcurrentHashMap<>();
    private final AtomicInteger expenseSequence = new AtomicInteger();

    void addUser(String id) {
        users.put(id, new User(id));
    }

    void createGroup(String id, String name, Collection<String> memberIds) {
        for (String memberId : memberIds) {
            if (!users.containsKey(memberId)) {
                throw new IllegalArgumentException("Unknown user: " + memberId);
            }
        }
        groups.put(id, new Group(id, name, memberIds));
    }

    Expense addExpense(String groupId, String description, String paidBy, Money total,
                       SplitStrategy split) {
        Group group = group(groupId);
        Map<String, Money> shares = split.shares(total);
        Expense expense = new Expense("E-" + expenseSequence.incrementAndGet(), description,
                paidBy, total, shares);
        group.addExpense(expense);
        return expense;
    }

    void settle(String groupId, String from, String to, Money amount) {
        group(groupId).settle(from, to, amount);
    }

    List<Debt> debts(String groupId) {
        return group(groupId).debts();
    }

    Map<String, Long> balances(String groupId) {
        return group(groupId).netBalances();
    }

    List<Debt> simplify(String groupId) {
        return DebtSimplifier.simplify(group(groupId).netBalances());
    }

    int expenseCount(String groupId) {
        return group(groupId).expenseCount();
    }

    private Group group(String groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupId);
        }
        return group;
    }
}
