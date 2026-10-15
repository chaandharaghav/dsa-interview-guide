import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Display console = System.out::println;
        InMemoryBank bank = new InMemoryBank();
        bank.openAccount("1111222233334444", "1234", 100_000);
        bank.openAccount("5555666677778888", "4321", 20_000);
        bank.openAccount("9999000011112222", "0000", 50_000);

        System.out.println("== ATM 1: one session, several transactions ==");
        CashDispenser notes1 = new CashDispenser();
        notes1.refill(Note.HUNDRED, 2);
        notes1.refill(Note.FIFTY, 4);
        notes1.refill(Note.TWENTY, 10);
        notes1.refill(Note.TEN, 10);
        Atm atm1 = new Atm("ATM1", bank, notes1, console, 50_000);
        System.out.println("State: " + atm1.stateName() + ", cash: "
                + Cents.format(notes1.total()));

        System.out.println("-- Actions without a card");
        attempt(() -> atm1.checkBalance());
        attempt(() -> atm1.withdraw(10_000));

        System.out.println("-- Wrong PIN, then the right PIN");
        atm1.insertCard("1111222233334444");
        atm1.enterPin("0000");
        System.out.println("State: " + atm1.stateName());
        atm1.enterPin("1234");
        System.out.println("State: " + atm1.stateName());
        atm1.checkBalance();

        System.out.println("-- Withdraw 270 dollars");
        atm1.withdraw(27_000);
        atm1.checkBalance();

        System.out.println("-- Withdrawals that the ATM declines");
        atm1.withdraw(1_500);
        atm1.withdraw(60_000);
        atm1.withdraw(0);
        System.out.println("-- Withdrawal that the bank declines");
        atm1.ejectCard();
        atm1.insertCard("5555666677778888");
        atm1.enterPin("4321");
        atm1.withdraw(30_000);
        atm1.checkBalance();
        attempt(() -> atm1.insertCard("1111222233334444"));
        atm1.ejectCard();

        System.out.println("-- Dispenser fault: the ATM reverses the debit");
        atm1.insertCard("1111222233334444");
        atm1.enterPin("1234");
        notes1.setJammed(true);
        atm1.withdraw(5_000);
        notes1.setJammed(false);
        atm1.checkBalance();
        atm1.ejectCard();

        System.out.println("-- Dispenser fault while the bank cannot take the reversal");
        atm1.insertCard("1111222233334444");
        atm1.enterPin("1234");
        notes1.setJammed(true);
        bank.setReversalsFailing(true);
        atm1.withdraw(5_000);
        notes1.setJammed(false);
        atm1.checkBalance();
        atm1.ejectCard();
        bank.setReversalsFailing(false);
        atm1.retryReversals();
        System.out.println("Balance after the retry: "
                + Cents.format(bank.balance("1111222233334444")));

        System.out.println("-- Three wrong PINs block the card");
        atm1.insertCard("9999000011112222");
        atm1.enterPin("1111");
        atm1.enterPin("2222");
        atm1.enterPin("3333");
        System.out.println("State: " + atm1.stateName());
        atm1.insertCard("9999000011112222");
        atm1.enterPin("0000");
        System.out.println("State: " + atm1.stateName());

        System.out.println("-- Transaction log of ATM1");
        for (LogEntry entry : atm1.log().entries()) {
            System.out.println(entry.format());
        }

        System.out.println();
        System.out.println("== ATM 2: few notes ==");
        CashDispenser notes2 = new CashDispenser();
        notes2.refill(Note.FIFTY, 2);
        notes2.refill(Note.TWENTY, 1);
        Atm atm2 = new Atm("ATM2", bank, notes2, console, 50_000);
        atm2.insertCard("5555666677778888");
        atm2.enterPin("4321");
        atm2.withdraw(3_000);
        atm2.withdraw(10_000);
        atm2.withdraw(2_000);
        System.out.println("State: " + atm2.stateName() + ", cash: "
                + Cents.format(notes2.total()));
        atm2.insertCard("5555666677778888");
        notes2.refill(Note.TEN, 5);
        atm2.refill();
        System.out.println("State: " + atm2.stateName() + ", cash: "
                + Cents.format(notes2.total()));

        System.out.println();
        System.out.println("== Bank: a repeated transaction id gets the first answer ==");
        String card = "1111222233334444";
        bank.withdraw(card, 5_000, "TEST-1");
        bank.withdraw(card, 5_000, "TEST-1");
        System.out.println("After two debits with id TEST-1: " + Cents.format(bank.balance(card)));
        bank.reverse("TEST-1");
        bank.reverse("TEST-1");
        System.out.println("After two reversals: " + Cents.format(bank.balance(card)));
        declineOrDebit(bank, card, "TEST-1");
        bank.reverse("TEST-2");
        declineOrDebit(bank, card, "TEST-2");
        System.out.println("Final balance: " + Cents.format(bank.balance(card)));

        System.out.println();
        System.out.println("== Two ATMs, one account with 100 dollars ==");
        InMemoryBank sharedBank = new InMemoryBank();
        sharedBank.openAccount("4444555566667777", "2468", 10_000);
        Atm left = atmWithCash("LEFT", sharedBank);
        Atm right = atmWithCash("RIGHT", sharedBank);
        Thread t1 = new Thread(() -> withdrawOnce(left, 8_000));
        Thread t2 = new Thread(() -> withdrawOnce(right, 8_000));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long approved = left.log().count(TxType.WITHDRAWAL, Outcome.SUCCESS)
                + right.log().count(TxType.WITHDRAWAL, Outcome.SUCCESS);
        System.out.println("Approved withdrawals: " + approved);
        System.out.println("Final balance: "
                + Cents.format(sharedBank.balance("4444555566667777")));
    }

    private static Atm atmWithCash(String id, BankService bank) {
        CashDispenser dispenser = new CashDispenser();
        dispenser.refill(Note.FIFTY, 10);
        dispenser.refill(Note.TWENTY, 10);
        dispenser.refill(Note.TEN, 10);
        return new Atm(id, bank, dispenser, message -> { }, 50_000);
    }

    private static void withdrawOnce(Atm atm, long cents) {
        atm.insertCard("4444555566667777");
        atm.enterPin("2468");
        atm.withdraw(cents);
        atm.ejectCard();
    }

    // A debit that arrives after the reversal of its transaction id must not debit.
    private static void declineOrDebit(BankService bank, String card, String transactionId) {
        try {
            bank.withdraw(card, 5_000, transactionId);
            System.out.println(transactionId + ": debited");
        } catch (DeclinedException e) {
            System.out.println(transactionId + ": declined, " + e.getMessage());
        }
    }

    private static void attempt(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

final class Cents {
    private Cents() {
    }

    static String format(long cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}

interface Display {
    void show(String message);
}

enum Note {
    HUNDRED(10_000), FIFTY(5_000), TWENTY(2_000), TEN(1_000);

    private final long cents;

    Note(long cents) {
        this.cents = cents;
    }

    long cents() {
        return cents;
    }

    String label() {
        return "$" + cents / 100;
    }

    static long smallest() {
        return TEN.cents;
    }
}

enum PinResult {
    OK, WRONG_PIN, BLOCKED
}

class DeclinedException extends RuntimeException {
    DeclinedException(String message) {
        super(message);
    }
}

// The bank did not answer. The caller does not know whether the bank applied the request.
class BankUnavailableException extends RuntimeException {
    BankUnavailableException(String message) {
        super(message);
    }
}

class DispenseException extends RuntimeException {
    DispenseException(String message) {
        super(message);
    }
}

interface BankService {
    PinResult verifyPin(String card, String pin);

    long balance(String card);

    // Debits the account. A repeated call with the same transaction id does not debit again:
    // it gets the answer of the first call. Throws BankUnavailableException if the bank does
    // not answer.
    void withdraw(String card, long cents, String transactionId) throws DeclinedException;

    // Credits back a debit that the ATM could not turn into cash. Safe to repeat. A reversal
    // of an id that the bank has not seen blocks a later debit with that id.
    void reverse(String transactionId);
}

class Account {
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final String pin;
    private long balance;
    private int failedAttempts;
    private boolean blocked;

    Account(String pin, long balance) {
        this.pin = pin;
        this.balance = balance;
    }

    synchronized PinResult checkPin(String candidate) {
        if (blocked) {
            return PinResult.BLOCKED;
        }
        if (pin.equals(candidate)) {
            failedAttempts = 0;
            return PinResult.OK;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            blocked = true;
            return PinResult.BLOCKED;
        }
        return PinResult.WRONG_PIN;
    }

    synchronized long balance() {
        return balance;
    }

    // The check and the change of the balance are one step, so two ATMs cannot both pass.
    synchronized boolean debit(long cents) {
        if (balance < cents) {
            return false;
        }
        balance -= cents;
        return true;
    }

    synchronized void credit(long cents) {
        balance += cents;
    }
}

class InMemoryBank implements BankService {
    private enum TxStatus { DEBITED, DECLINED, REVERSED }

    private record Transaction(String card, long cents, TxStatus status) { }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    // One record for each transaction id. The bank never removes a record, so a repeated
    // request gets the first answer.
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private volatile boolean reversalsFailing;

    void openAccount(String card, String pin, long balance) {
        accounts.put(card, new Account(pin, balance));
    }

    // Test hook: simulates a network fault on the reversal path.
    void setReversalsFailing(boolean failing) {
        this.reversalsFailing = failing;
    }

    @Override
    public PinResult verifyPin(String card, String pin) {
        Account account = accounts.get(card);
        return account == null ? PinResult.BLOCKED : account.checkPin(pin);
    }

    @Override
    public long balance(String card) {
        return accounts.get(card).balance();
    }

    @Override
    public void withdraw(String card, long cents, String transactionId) {
        // computeIfAbsent is atomic for one key: the debit runs at most once for each id.
        Transaction tx = transactions.computeIfAbsent(transactionId, id -> {
            boolean debited = accounts.get(card).debit(cents);
            return new Transaction(card, cents, debited ? TxStatus.DEBITED : TxStatus.DECLINED);
        });
        switch (tx.status()) {
            case DEBITED -> { }
            case DECLINED -> throw new DeclinedException("Insufficient funds");
            case REVERSED -> throw new DeclinedException("The transaction was reversed");
        }
    }

    @Override
    public void reverse(String transactionId) {
        if (reversalsFailing) {
            throw new BankUnavailableException("The bank did not answer");
        }
        transactions.compute(transactionId, (id, tx) -> {
            if (tx == null) {
                // The debit request has not arrived. The record blocks it if it arrives later.
                return new Transaction(null, 0, TxStatus.REVERSED);
            }
            if (tx.status() == TxStatus.DEBITED) {
                accounts.get(tx.card()).credit(tx.cents());
                return new Transaction(tx.card(), tx.cents(), TxStatus.REVERSED);
            }
            return tx;
        });
    }
}

enum TxType {
    PIN, BALANCE, WITHDRAWAL, REVERSAL
}

enum Outcome {
    SUCCESS, DECLINED, FAILED
}

record LogEntry(int sequence, String card, TxType type, Outcome outcome, long cents,
                String detail) {
    String format() {
        return "#" + sequence + " " + card + " " + type + " " + outcome + " "
                + (cents == 0 ? "-" : Cents.format(cents)) + " " + detail;
    }
}

class TransactionLog {
    private final List<LogEntry> entries = new ArrayList<>();

    synchronized void add(String card, TxType type, Outcome outcome, long cents, String detail) {
        String masked = "****" + card.substring(card.length() - 4);
        entries.add(new LogEntry(entries.size() + 1, masked, type, outcome, cents, detail));
    }

    synchronized List<LogEntry> entries() {
        return List.copyOf(entries);
    }

    synchronized long count(TxType type, Outcome outcome) {
        return entries.stream().filter(e -> e.type() == type && e.outcome() == outcome).count();
    }
}

// One link of the chain. It takes as many notes of its value as it can, and passes the rest on.
class NoteHandler {
    private final Note note;
    private final NoteHandler next;

    NoteHandler(Note note, NoteHandler next) {
        this.note = note;
        this.next = next;
    }

    long handle(long remaining, Map<Note, Integer> stock, Map<Note, Integer> plan) {
        int count = (int) Math.min(remaining / note.cents(), stock.getOrDefault(note, 0));
        if (count > 0) {
            plan.put(note, count);
        }
        long left = remaining - count * note.cents();
        return next == null ? left : next.handle(left, stock, plan);
    }
}

class CashDispenser {
    private final Map<Note, Integer> stock = new EnumMap<>(Note.class);
    private final NoteHandler chain = buildChain();
    private boolean jammed;

    // Notes are declared from the largest to the smallest. The chain starts at the largest.
    private static NoteHandler buildChain() {
        Note[] notes = Note.values();
        NoteHandler handler = null;
        for (int i = notes.length - 1; i >= 0; i--) {
            handler = new NoteHandler(notes[i], handler);
        }
        return handler;
    }

    synchronized void refill(Note note, int count) {
        stock.merge(note, count, Integer::sum);
    }

    synchronized void setJammed(boolean jammed) {
        this.jammed = jammed;
    }

    synchronized long total() {
        return stock.entrySet().stream().mapToLong(e -> e.getKey().cents() * e.getValue()).sum();
    }

    // Finds the notes for the amount. Does not change the stock.
    synchronized Optional<Map<Note, Integer>> plan(long cents) {
        Map<Note, Integer> plan = new EnumMap<>(Note.class);
        long left = chain.handle(cents, stock, plan);
        return left == 0 ? Optional.of(plan) : Optional.empty();
    }

    synchronized void commit(Map<Note, Integer> plan) {
        if (jammed) {
            throw new DispenseException("The dispenser is jammed");
        }
        for (Map.Entry<Note, Integer> entry : plan.entrySet()) {
            if (stock.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                throw new DispenseException("The notes are no longer in the machine");
            }
        }
        plan.forEach((note, count) -> stock.merge(note, -count, Integer::sum));
    }

    static String describe(Map<Note, Integer> plan) {
        return plan.entrySet().stream()
                .map(e -> e.getValue() + " x " + e.getKey().label())
                .collect(Collectors.joining(", "));
    }
}

interface AtmState {
    String name();

    default void insertCard(Atm atm, String card) {
        throw new IllegalStateException("Cannot insert a card in state " + name());
    }

    default void enterPin(Atm atm, String pin) {
        throw new IllegalStateException("Cannot enter a PIN in state " + name());
    }

    default void checkBalance(Atm atm) {
        throw new IllegalStateException("Cannot check the balance in state " + name());
    }

    default void withdraw(Atm atm, long cents) {
        throw new IllegalStateException("Cannot withdraw in state " + name());
    }

    default void ejectCard(Atm atm) {
        throw new IllegalStateException("Cannot eject a card in state " + name());
    }
}

class IdleState implements AtmState {
    @Override
    public String name() {
        return "Idle";
    }

    @Override
    public void insertCard(Atm atm, String card) {
        atm.startSession(card);
        atm.display().show("Card accepted. Enter your PIN");
        atm.setState(atm.hasCardState());
    }
}

class HasCardState implements AtmState {
    @Override
    public String name() {
        return "HasCard";
    }

    @Override
    public void enterPin(Atm atm, String pin) {
        switch (atm.bank().verifyPin(atm.card(), pin)) {
            case OK -> {
                atm.report(TxType.PIN, Outcome.SUCCESS, 0, "PIN accepted");
                atm.setState(atm.authenticatedState());
            }
            case WRONG_PIN -> atm.report(TxType.PIN, Outcome.DECLINED, 0, "Wrong PIN");
            case BLOCKED -> {
                atm.report(TxType.PIN, Outcome.DECLINED, 0, "Card not accepted");
                atm.endSession("Card retained. Contact your bank");
            }
        }
    }

    @Override
    public void ejectCard(Atm atm) {
        atm.endSession("Card returned");
    }
}

class AuthenticatedState implements AtmState {
    @Override
    public String name() {
        return "Authenticated";
    }

    @Override
    public void checkBalance(Atm atm) {
        long balance = atm.bank().balance(atm.card());
        atm.report(TxType.BALANCE, Outcome.SUCCESS, 0, "Balance " + Cents.format(balance));
    }

    @Override
    public void withdraw(Atm atm, long cents) {
        if (cents <= 0 || cents % Note.smallest() != 0) {
            atm.report(TxType.WITHDRAWAL, Outcome.DECLINED, cents,
                    "Enter a multiple of " + Cents.format(Note.smallest()));
            return;
        }
        if (cents > atm.limit()) {
            atm.report(TxType.WITHDRAWAL, Outcome.DECLINED, cents,
                    "The limit for one withdrawal is " + Cents.format(atm.limit()));
            return;
        }
        Optional<Map<Note, Integer>> plan = atm.dispenser().plan(cents);
        if (plan.isEmpty()) {
            atm.report(TxType.WITHDRAWAL, Outcome.DECLINED, cents,
                    "This machine cannot pay that amount with its notes");
            return;
        }
        Map<Note, Integer> notes = plan.get();
        String transactionId = atm.nextTransactionId();
        try {
            atm.bank().withdraw(atm.card(), cents, transactionId);
        } catch (DeclinedException e) {
            atm.report(TxType.WITHDRAWAL, Outcome.DECLINED, cents, e.getMessage());
            return;
        } catch (BankUnavailableException e) {
            // The bank may have debited the account. The reversal undoes the debit if it exists.
            boolean reversed = atm.reverseOrQueue(transactionId);
            atm.report(TxType.WITHDRAWAL, Outcome.FAILED, cents, "The bank did not answer."
                    + (reversed ? " No cash, no debit" : " No cash. The reversal is pending"));
            return;
        }
        try {
            atm.dispenser().commit(notes);
        } catch (DispenseException e) {
            boolean reversed = atm.reverseOrQueue(transactionId);
            atm.report(TxType.WITHDRAWAL, Outcome.FAILED, cents, e.getMessage()
                    + (reversed ? ". The debit was reversed" : ". The reversal is pending"));
            return;
        }
        atm.report(TxType.WITHDRAWAL, Outcome.SUCCESS, cents,
                "Dispensed " + Cents.format(cents) + ": " + CashDispenser.describe(notes));
        if (atm.dispenser().total() == 0) {
            atm.endSession("Card returned. Out of cash");
        }
    }

    @Override
    public void ejectCard(Atm atm) {
        atm.endSession("Card returned");
    }
}

class OutOfServiceState implements AtmState {
    @Override
    public String name() {
        return "OutOfService";
    }

    @Override
    public void insertCard(Atm atm, String card) {
        atm.display().show("Out of service. Card not accepted");
    }
}

class Atm {
    private record PendingReversal(String transactionId, String card) { }

    private final String id;
    private final BankService bank;
    private final CashDispenser dispenser;
    private final Display display;
    private final TransactionLog log = new TransactionLog();
    private final long limit;
    private final AtmState idle = new IdleState();
    private final AtmState hasCard = new HasCardState();
    private final AtmState authenticated = new AuthenticatedState();
    private final AtmState outOfService = new OutOfServiceState();
    private final List<PendingReversal> pendingReversals = new ArrayList<>();
    private AtmState state;
    private String card;
    private int transactionCounter;

    Atm(String id, BankService bank, CashDispenser dispenser, Display display, long limit) {
        this.id = id;
        this.bank = bank;
        this.dispenser = dispenser;
        this.display = display;
        this.limit = limit;
        this.state = dispenser.total() > 0 ? idle : outOfService;
    }

    // Public operations: each one holds the ATM lock and delegates to the current state.
    public synchronized void insertCard(String card) {
        state.insertCard(this, card);
    }

    public synchronized void enterPin(String pin) {
        state.enterPin(this, pin);
    }

    public synchronized void checkBalance() {
        state.checkBalance(this);
    }

    public synchronized void withdraw(long cents) {
        state.withdraw(this, cents);
    }

    public synchronized void ejectCard() {
        state.ejectCard(this);
    }

    // The operator calls this after loading notes into the dispenser.
    public synchronized void refill() {
        if (state == outOfService && dispenser.total() > 0) {
            state = idle;
        }
    }

    // A timer or the operator calls this. It sends each pending reversal again, in order.
    public synchronized void retryReversals() {
        Iterator<PendingReversal> it = pendingReversals.iterator();
        while (it.hasNext()) {
            PendingReversal pending = it.next();
            try {
                bank.reverse(pending.transactionId());
            } catch (BankUnavailableException e) {
                return;
            }
            it.remove();
            display.show("Reversal " + pending.transactionId() + " accepted by the bank");
            log.add(pending.card(), TxType.REVERSAL, Outcome.SUCCESS, 0,
                    "Reversal of " + pending.transactionId());
        }
    }

    // Hooks for the states. They run inside a public method, so they hold the ATM lock.
    void setState(AtmState next) {
        this.state = next;
    }

    void startSession(String card) {
        this.card = card;
    }

    // Ends the session and chooses the next state from the cash that is left.
    void endSession(String message) {
        display.show(message);
        card = null;
        state = dispenser.total() > 0 ? idle : outOfService;
    }

    // Shows a message to the customer and writes the same event to the transaction log.
    void report(TxType type, Outcome outcome, long cents, String message) {
        display.show(message);
        log.add(card, type, outcome, cents, message);
    }

    // Sends the reversal. If the bank does not answer, keeps it for retryReversals.
    boolean reverseOrQueue(String transactionId) {
        try {
            bank.reverse(transactionId);
            return true;
        } catch (BankUnavailableException e) {
            pendingReversals.add(new PendingReversal(transactionId, card));
            return false;
        }
    }

    String nextTransactionId() {
        transactionCounter++;
        return id + "-" + transactionCounter;
    }

    String stateName() {
        return state.name();
    }

    String card() {
        return card;
    }

    BankService bank() {
        return bank;
    }

    CashDispenser dispenser() {
        return dispenser;
    }

    Display display() {
        return display;
    }

    TransactionLog log() {
        return log;
    }

    long limit() {
        return limit;
    }

    AtmState hasCardState() {
        return hasCard;
    }

    AtmState authenticatedState() {
        return authenticated;
    }
}
