import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) throws Exception {
        ManualClock clock = new ManualClock(Instant.parse("2026-01-01T00:00:00Z"));
        WalletService wallet = new WalletService(clock);

        System.out.println("== 1. Open accounts and deposit ==");
        wallet.openAccount("alice");
        wallet.openAccount("bob");
        System.out.println(wallet.deposit("dep-1", "alice", 10_000));
        System.out.println("alice=" + wallet.balance("alice") + " external="
                + wallet.balance(WalletService.EXTERNAL));

        System.out.println("== 2. Transfer and history ==");
        clock.advance(Duration.ofSeconds(30));
        System.out.println(wallet.transfer("pay-1", "alice", "bob", 2_500));
        clock.advance(Duration.ofSeconds(30));
        System.out.println(wallet.withdraw("wd-1", "bob", 500));
        System.out.println("alice=" + wallet.balance("alice") + " bob=" + wallet.balance("bob"));
        for (LedgerEntry entry : wallet.history("bob", 10)) {
            System.out.println("bob " + entry);
        }

        System.out.println("== 3. Rejected transfers ==");
        System.out.println(wallet.transfer("pay-2", "bob", "alice", 5_000));
        System.out.println(wallet.transfer("pay-3", "bob", "nobody", 100));
        System.out.println(wallet.transfer("pay-4", "bob", "bob", 100));
        System.out.println(wallet.transfer("pay-5", "bob", "alice", 0));
        System.out.println("alice=" + wallet.balance("alice") + " bob=" + wallet.balance("bob"));

        System.out.println("== 4. Idempotency keys ==");
        TransferResult first = wallet.transfer("pay-6", "alice", "bob", 1_000);
        TransferResult retry = wallet.transfer("pay-6", "alice", "bob", 1_000);
        System.out.println("first:  " + first);
        System.out.println("retry:  " + retry);
        System.out.println("alice=" + wallet.balance("alice") + " bob=" + wallet.balance("bob"));
        try {
            wallet.transfer("pay-6", "alice", "bob", 9_999);
        } catch (IdempotencyConflictException e) {
            System.out.println("conflict: " + e.getMessage());
        }
        TransferResult declined = wallet.transfer("pay-2", "bob", "alice", 5_000);
        System.out.println("declined transfer replayed: " + declined.replayed());

        System.out.println("== 5. Eight threads send the same key ==");
        wallet.openAccount("carol");
        List<TransferResult> results = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread t = new Thread(() -> {
                TransferResult r = wallet.transfer("pay-7", "alice", "carol", 700);
                synchronized (results) {
                    results.add(r);
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long replayed = results.stream().filter(TransferResult::replayed).count();
        System.out.println("results=" + results.size() + " executed=" + (results.size() - replayed)
                + " replayed=" + replayed);
        System.out.println("carol=" + wallet.balance("carol") + " entries="
                + wallet.history("carol", 10).size());

        System.out.println("== 6. Concurrent transfers with lock ordering ==");
        List<String> ids = List.of("a1", "a2", "a3", "a4");
        for (String id : ids) {
            wallet.openAccount(id);
            wallet.deposit("seed-" + id, id, 10_000_000);
        }
        threads.clear();
        for (int t = 0; t < 8; t++) {
            final int seed = t;
            Thread worker = new Thread(() -> {
                Random random = new Random(seed);
                for (int i = 0; i < 2_000; i++) {
                    String from = ids.get(random.nextInt(ids.size()));
                    String to = ids.get(random.nextInt(ids.size()));
                    long amount = 1 + random.nextInt(100);
                    wallet.transfer("w" + seed + "-" + i, from, to, amount);
                }
            });
            threads.add(worker);
            worker.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        for (String id : ids) {
            System.out.println(id + "=" + wallet.balance(id));
        }
        System.out.println("ledger sum=" + wallet.ledgerSum());
        System.out.println("every balance equals its entries: " + wallet.reconciles());
    }
}

/** A clock that only moves when the demo calls advance. */
final class ManualClock extends Clock {
    private final Instant start;
    private final AtomicLong offsetMillis = new AtomicLong();

    ManualClock(Instant start) {
        this.start = start;
    }

    void advance(Duration duration) {
        offsetMillis.addAndGet(duration.toMillis());
    }

    @Override
    public Instant instant() {
        return start.plusMillis(offsetMillis.get());
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}

/** One line of the ledger. A negative amount is a debit. A positive amount is a credit. */
record LedgerEntry(long seq, String txId, String accountId, long amount, long balanceAfter,
                   String memo, Instant at) {
    @Override
    public String toString() {
        return "#" + seq + " " + txId + " " + (amount < 0 ? "DEBIT " : "CREDIT ")
                + Math.abs(amount) + " balance=" + balanceAfter + " (" + memo + ")";
    }
}

enum TransferStatus { COMPLETED, REJECTED }

record TransferRequest(String fromId, String toId, long amount) {
}

record TransferResult(TransferStatus status, String txId, String reason, boolean replayed) {
    static TransferResult completed(String txId) {
        return new TransferResult(TransferStatus.COMPLETED, txId, "", false);
    }

    static TransferResult rejected(String reason) {
        return new TransferResult(TransferStatus.REJECTED, "-", reason, false);
    }

    TransferResult asReplay() {
        return new TransferResult(status, txId, reason, true);
    }
}

final class IdempotencyConflictException extends RuntimeException {
    IdempotencyConflictException(String key) {
        super("idempotency key " + key + " was used with a different request");
    }
}

/** An account. Its balance and its entries change only while a caller holds its monitor. */
final class Account {
    private final String id;
    private final boolean allowsOverdraft;
    private long balance;                                      // guarded by this
    private final List<LedgerEntry> entries = new ArrayList<>(); // guarded by this

    Account(String id, boolean allowsOverdraft) {
        this.id = id;
        this.allowsOverdraft = allowsOverdraft;
    }

    String id() {
        return id;
    }

    /** The caller must hold the monitor of this account. */
    boolean canDebit(long amount) {
        return allowsOverdraft || balance >= amount;
    }

    /** The caller must hold the monitor of this account. */
    void post(long seq, String txId, long amount, String memo, Instant at) {
        balance += amount;
        entries.add(new LedgerEntry(seq, txId, id, amount, balance, memo, at));
    }

    synchronized long balance() {
        return balance;
    }

    synchronized List<LedgerEntry> entriesNewestFirst(int limit) {
        List<LedgerEntry> copy = new ArrayList<>(entries);
        Collections.reverse(copy);
        return copy.subList(0, Math.min(limit, copy.size()));
    }

    synchronized long entrySum() {
        long sum = 0;
        for (LedgerEntry e : entries) {
            sum += e.amount();
        }
        return sum;
    }
}

/** The wallet. It opens accounts, moves money with double entries, and replays retries. */
final class WalletService {
    /** The system account for money that enters or leaves the wallet. It may go negative. */
    static final String EXTERNAL = "EXTERNAL";

    private record Pending(TransferRequest request, CompletableFuture<TransferResult> outcome) {
    }

    private final Clock clock;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Pending> idempotency = new ConcurrentHashMap<>();
    private final AtomicLong txSeq = new AtomicLong();
    private final AtomicLong entrySeq = new AtomicLong();

    WalletService(Clock clock) {
        this.clock = clock;
        accounts.put(EXTERNAL, new Account(EXTERNAL, true));
    }

    void openAccount(String id) {
        if (accounts.putIfAbsent(id, new Account(id, false)) != null) {
            throw new IllegalArgumentException("account " + id + " exists");
        }
    }

    long balance(String id) {
        return accounts.get(id).balance();
    }

    List<LedgerEntry> history(String id, int limit) {
        return accounts.get(id).entriesNewestFirst(limit);
    }

    TransferResult deposit(String key, String accountId, long amount) {
        return transfer(key, EXTERNAL, accountId, amount);
    }

    TransferResult withdraw(String key, String accountId, long amount) {
        return transfer(key, accountId, EXTERNAL, amount);
    }

    /** Runs the transfer once for each key. A retry with the same key gets the first result. */
    TransferResult transfer(String key, String fromId, String toId, long amount) {
        TransferRequest request = new TransferRequest(fromId, toId, amount);
        Pending mine = new Pending(request, new CompletableFuture<>());
        Pending existing = idempotency.putIfAbsent(key, mine);
        if (existing == null) {
            try {
                TransferResult result = execute(request);
                mine.outcome().complete(result);
                return result;
            } catch (RuntimeException | Error e) {
                // An unexpected failure frees the key, so a later retry runs again.
                // This is safe only if execute changed nothing before it threw.
                idempotency.remove(key, mine);
                mine.outcome().completeExceptionally(e);   // waiters get a CompletionException
                throw e;
            }
        }
        if (!existing.request().equals(request)) {
            throw new IdempotencyConflictException(key);
        }
        return existing.outcome().join().asReplay();
    }

    private TransferResult execute(TransferRequest request) {
        if (request.amount() <= 0) {
            return TransferResult.rejected("amount must be positive");
        }
        if (request.fromId().equals(request.toId())) {
            return TransferResult.rejected("same account");
        }
        Account from = accounts.get(request.fromId());
        Account to = accounts.get(request.toId());
        if (from == null || to == null) {
            return TransferResult.rejected("unknown account");
        }
        // Lock ordering: every transfer locks the account with the smaller id first.
        Account first = from.id().compareTo(to.id()) < 0 ? from : to;
        Account second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (!from.canDebit(request.amount())) {
                    return TransferResult.rejected("insufficient funds");
                }
                String txId = "TX-" + txSeq.incrementAndGet();
                Instant now = clock.instant();
                from.post(entrySeq.incrementAndGet(), txId, -request.amount(),
                        "to " + to.id(), now);
                to.post(entrySeq.incrementAndGet(), txId, request.amount(),
                        "from " + from.id(), now);
                return TransferResult.completed(txId);
            }
        }
    }

    /** Sum of all ledger entries. Double entry makes it zero. */
    long ledgerSum() {
        long sum = 0;
        for (Account account : accounts.values()) {
            sum += account.entrySum();
        }
        return sum;
    }

    /** Checks that each balance equals the sum of its entries. */
    boolean reconciles() {
        for (Account account : accounts.values()) {
            if (account.balance() != account.entrySum()) {
                return false;
            }
        }
        return true;
    }
}
