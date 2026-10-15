import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        ManualClock clock = new ManualClock(LocalDate.of(2026, 3, 1));
        List<String> inbox = new ArrayList<>();
        FineStrategy fines = new CappedFine(new PerDayFine(new BigDecimal("0.50")),
                new BigDecimal("10.00"));
        Library library = new Library(clock, fines, 2, 14, 3);
        library.addListener((member, book) ->
                inbox.add(member.name() + ": '" + book.title() + "' is ready to borrow"));

        System.out.println("== Catalog and search ==");
        library.addBook("111", "Clean Code", "Robert Martin", 2);
        library.addBook("222", "Effective Java", "Joshua Bloch", 1);
        library.addBook("333", "Java Concurrency in Practice", "Brian Goetz", 1);
        library.addBook("444", "Refactoring", "Martin Fowler", 1);
        library.register("alice", "Alice");
        library.register("bob", "Bob");
        library.register("carol", "Carol");
        library.register("dave", "Dave");
        library.register("erin", "Erin");
        System.out.println("title 'java': " + titles(library.searchByTitle("java")));
        System.out.println("title 'java concurrency': "
                + titles(library.searchByTitle("java concurrency")));
        System.out.println("author 'martin': " + titles(library.searchByAuthor("martin")));

        System.out.println("== Loan, reservation, late return (day 0) ==");
        Loan first = library.checkout("alice", "222");
        System.out.println("alice borrows Effective Java, due " + first.dueOn());
        attempt("bob borrows Effective Java", () -> library.checkout("bob", "222"));
        library.reserve("bob", "222");
        library.reserve("carol", "222");
        System.out.println("reservations for 222: " + library.waiting("222"));

        clock.advanceDays(20);
        System.out.println("== Day 20: alice returns late ==");
        BigDecimal fine = library.returnCopy(first.copyId());
        System.out.println("fine: " + fine + ", unpaid: " + library.unpaidFine("alice"));
        System.out.println("inbox: " + inbox);
        attempt("alice borrows Clean Code with unpaid fine",
                () -> library.checkout("alice", "111"));
        attempt("alice pays a negative amount",
                () -> library.payFine("alice", new BigDecimal("-1.00")));
        library.payFine("alice", new BigDecimal("3.00"));
        library.checkout("alice", "111");
        library.checkout("alice", "333");
        attempt("alice borrows a third book", () -> library.checkout("alice", "222"));

        clock.advanceDays(5);
        System.out.println("== Day 25: bob's hold expired ==");
        attempt("bob borrows Effective Java", () -> library.checkout("bob", "222"));
        Loan carolLoan = library.checkout("carol", "222");
        System.out.println("carol borrows Effective Java, due " + carolLoan.dueOn());
        System.out.println("inbox: " + inbox);

        System.out.println("== Concurrent checkout of the last copy ==");
        AtomicInteger wins = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (String id : List.of("dave", "erin")) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    library.checkout(id, "444");
                    wins.incrementAndGet();
                } catch (LibraryException e) {
                    // The thread that loses the race gets a LibraryException.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(t);
            t.start();
        }
        start.countDown();
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("successful checkouts of 444: " + wins.get());
        System.out.println("available copies of 444: " + library.availableCopies("444"));
        library.returnCopy("444-1");

        clock.advanceDays(15);
        System.out.println("== Day 40: overdue report ==");
        for (String line : library.overdueReport()) {
            System.out.println(line);
        }
    }

    private static List<String> titles(List<Book> books) {
        return books.stream().map(Book::title).toList();
    }

    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": ok");
        } catch (LibraryException e) {
            System.out.println(label + ": rejected (" + e.getMessage() + ")");
        }
    }
}

class LibraryException extends RuntimeException {
    LibraryException(String message) {
        super(message);
    }
}

record Book(String isbn, String title, String author) { }

record Member(String id, String name) { }

enum CopyStatus { AVAILABLE, ON_LOAN, HELD }

final class BookCopy {
    final String copyId;
    final String isbn;
    CopyStatus status = CopyStatus.AVAILABLE;
    String heldFor;
    LocalDate heldUntil;

    BookCopy(String copyId, String isbn) {
        this.copyId = copyId;
        this.isbn = isbn;
    }
}

final class Loan {
    private final String copyId;
    private final String memberId;
    private final LocalDate dueOn;

    Loan(String copyId, String memberId, LocalDate dueOn) {
        this.copyId = copyId;
        this.memberId = memberId;
        this.dueOn = dueOn;
    }

    String copyId() { return copyId; }
    String memberId() { return memberId; }
    LocalDate dueOn() { return dueOn; }
}

interface FineStrategy {
    BigDecimal fine(long daysLate);
}

record PerDayFine(BigDecimal perDay) implements FineStrategy {
    public BigDecimal fine(long daysLate) {
        return perDay.multiply(BigDecimal.valueOf(daysLate)).setScale(2, RoundingMode.HALF_UP);
    }
}

record CappedFine(FineStrategy inner, BigDecimal cap) implements FineStrategy {
    public BigDecimal fine(long daysLate) {
        return inner.fine(daysLate).min(cap);
    }
}

interface AvailabilityListener {
    void onHoldReady(Member member, Book book);
}

final class ManualClock extends Clock {
    private volatile LocalDate today;

    ManualClock(LocalDate start) {
        this.today = start;
    }

    void advanceDays(int days) {
        today = today.plusDays(days);
    }

    @Override public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override public Instant instant() {
        return today.atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}

final class SearchIndex {
    private final Map<String, Set<String>> titleTokens = new TreeMap<>();
    private final Map<String, Set<String>> authorTokens = new TreeMap<>();

    void add(Book book) {
        for (String token : tokens(book.title())) {
            titleTokens.computeIfAbsent(token, k -> new TreeSet<>()).add(book.isbn());
        }
        for (String token : tokens(book.author())) {
            authorTokens.computeIfAbsent(token, k -> new TreeSet<>()).add(book.isbn());
        }
    }

    Set<String> byTitle(String query) {
        return lookup(titleTokens, query);
    }

    Set<String> byAuthor(String query) {
        return lookup(authorTokens, query);
    }

    private static Set<String> lookup(Map<String, Set<String>> index, String query) {
        Set<String> result = null;
        for (String token : tokens(query)) {
            Set<String> hits = index.getOrDefault(token, Set.of());
            if (result == null) {
                result = new TreeSet<>(hits);
            } else {
                result.retainAll(hits);
            }
        }
        return result == null ? new TreeSet<>() : result;
    }

    private static List<String> tokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> !token.isEmpty())
                .toList();
    }
}

final class Library {
    private final Clock clock;
    private final FineStrategy fineStrategy;
    private final int maxLoans;
    private final int loanDays;
    private final int holdDays;
    private final Map<String, Book> books = new TreeMap<>();
    private final Map<String, List<BookCopy>> copies = new TreeMap<>();
    private final Map<String, BookCopy> copiesById = new TreeMap<>();
    private final Map<String, Member> members = new TreeMap<>();
    private final Map<String, Loan> loansByCopy = new TreeMap<>();
    private final Map<String, Deque<String>> waitingByIsbn = new TreeMap<>();
    private final Map<String, BigDecimal> unpaid = new TreeMap<>();
    private final SearchIndex index = new SearchIndex();
    private final List<AvailabilityListener> listeners = new ArrayList<>();

    Library(Clock clock, FineStrategy fineStrategy, int maxLoans, int loanDays, int holdDays) {
        this.clock = clock;
        this.fineStrategy = fineStrategy;
        this.maxLoans = maxLoans;
        this.loanDays = loanDays;
        this.holdDays = holdDays;
    }

    synchronized void addListener(AvailabilityListener listener) {
        listeners.add(listener);
    }

    synchronized void addBook(String isbn, String title, String author, int copyCount) {
        Book book = books.computeIfAbsent(isbn, k -> new Book(isbn, title, author));
        index.add(book);
        List<BookCopy> list = copies.computeIfAbsent(isbn, k -> new ArrayList<>());
        for (int i = 0; i < copyCount; i++) {
            BookCopy copy = new BookCopy(isbn + "-" + (list.size() + 1), isbn);
            list.add(copy);
            copiesById.put(copy.copyId, copy);
        }
        assignHolds(isbn);
    }

    synchronized void register(String id, String name) {
        if (members.putIfAbsent(id, new Member(id, name)) != null) {
            throw new LibraryException("member " + id + " exists");
        }
    }

    synchronized Loan checkout(String memberId, String isbn) {
        refreshHolds();
        Member member = member(memberId);
        if (unpaidFine(memberId).signum() > 0) {
            throw new LibraryException("unpaid fine " + unpaidFine(memberId));
        }
        if (activeLoans(memberId) >= maxLoans) {
            throw new LibraryException("loan limit " + maxLoans + " reached");
        }
        BookCopy copy = findBorrowable(isbn, member.id());
        copy.status = CopyStatus.ON_LOAN;
        copy.heldFor = null;
        copy.heldUntil = null;
        Loan loan = new Loan(copy.copyId, memberId, LocalDate.now(clock).plusDays(loanDays));
        loansByCopy.put(copy.copyId, loan);
        return loan;
    }

    synchronized BigDecimal returnCopy(String copyId) {
        Loan loan = loansByCopy.remove(copyId);
        if (loan == null) {
            throw new LibraryException("copy " + copyId + " is not on loan");
        }
        long daysLate = ChronoUnit.DAYS.between(loan.dueOn(), LocalDate.now(clock));
        BigDecimal fine = daysLate > 0 ? fineStrategy.fine(daysLate) : BigDecimal.ZERO;
        if (fine.signum() > 0) {
            unpaid.merge(loan.memberId(), fine, BigDecimal::add);
        }
        BookCopy copy = copyById(copyId);
        copy.status = CopyStatus.AVAILABLE;
        assignHolds(copy.isbn);
        return fine;
    }

    synchronized void reserve(String memberId, String isbn) {
        refreshHolds();
        member(memberId);
        if (!books.containsKey(isbn)) {
            throw new LibraryException("unknown book " + isbn);
        }
        if (availableCopies(isbn) > 0) {
            throw new LibraryException("a copy is available, borrow it");
        }
        Deque<String> queue = waitingByIsbn.computeIfAbsent(isbn, k -> new ArrayDeque<>());
        if (queue.contains(memberId)) {
            throw new LibraryException("already waiting");
        }
        queue.addLast(memberId);
    }

    synchronized void payFine(String memberId, BigDecimal amount) {
        if (amount.signum() <= 0 || amount.scale() > 2) {
            throw new LibraryException("payment must be positive, with at most 2 decimals");
        }
        BigDecimal rest = unpaidFine(memberId).subtract(amount);
        if (rest.signum() < 0) {
            throw new LibraryException("payment is larger than the fine");
        }
        unpaid.put(memberId, rest);
    }

    synchronized BigDecimal unpaidFine(String memberId) {
        return unpaid.getOrDefault(memberId, BigDecimal.ZERO).setScale(2);
    }

    synchronized List<Book> searchByTitle(String query) {
        return index.byTitle(query).stream().map(books::get).toList();
    }

    synchronized List<Book> searchByAuthor(String query) {
        return index.byAuthor(query).stream().map(books::get).toList();
    }

    synchronized int availableCopies(String isbn) {
        refreshHolds();
        return (int) copies.getOrDefault(isbn, List.of()).stream()
                .filter(c -> c.status == CopyStatus.AVAILABLE).count();
    }

    synchronized List<String> waiting(String isbn) {
        refreshHolds();
        return List.copyOf(waitingByIsbn.getOrDefault(isbn, new ArrayDeque<>()));
    }

    synchronized List<String> overdueReport() {
        LocalDate today = LocalDate.now(clock);
        return loansByCopy.values().stream()
                .filter(loan -> loan.dueOn().isBefore(today))
                .sorted(Comparator.comparing(Loan::dueOn).thenComparing(Loan::copyId))
                .map(loan -> {
                    long late = ChronoUnit.DAYS.between(loan.dueOn(), today);
                    String title = books.get(copyById(loan.copyId()).isbn).title();
                    return member(loan.memberId()).name() + " | " + title
                            + " | days late: " + late + " | fine so far " + fineStrategy.fine(late);
                })
                .toList();
    }

    private BookCopy findBorrowable(String isbn, String memberId) {
        if (!books.containsKey(isbn)) {
            throw new LibraryException("unknown book " + isbn);
        }
        List<BookCopy> list = copies.get(isbn);
        for (BookCopy copy : list) {
            if (copy.status == CopyStatus.HELD && memberId.equals(copy.heldFor)) {
                return copy;
            }
        }
        for (BookCopy copy : list) {
            if (copy.status == CopyStatus.AVAILABLE) {
                return copy;
            }
        }
        throw new LibraryException("no copy available, reserve the book");
    }

    private void assignHolds(String isbn) {
        Deque<String> queue = waitingByIsbn.get(isbn);
        for (BookCopy copy : copies.get(isbn)) {
            if (queue == null || queue.isEmpty()) {
                return;
            }
            if (copy.status == CopyStatus.AVAILABLE) {
                copy.status = CopyStatus.HELD;
                copy.heldFor = queue.pollFirst();
                copy.heldUntil = LocalDate.now(clock).plusDays(holdDays);
                Member member = members.get(copy.heldFor);
                listeners.forEach(l -> l.onHoldReady(member, books.get(isbn)));
            }
        }
    }

    private void refreshHolds() {
        LocalDate today = LocalDate.now(clock);
        for (List<BookCopy> list : copies.values()) {
            for (BookCopy copy : list) {
                if (copy.status == CopyStatus.HELD && today.isAfter(copy.heldUntil)) {
                    copy.status = CopyStatus.AVAILABLE;
                    copy.heldFor = null;
                    copy.heldUntil = null;
                    assignHolds(copy.isbn);
                }
            }
        }
    }

    private int activeLoans(String memberId) {
        return (int) loansByCopy.values().stream()
                .filter(loan -> loan.memberId().equals(memberId)).count();
    }

    private Member member(String id) {
        Member member = members.get(id);
        if (member == null) {
            throw new LibraryException("unknown member " + id);
        }
        return member;
    }

    private BookCopy copyById(String copyId) {
        BookCopy copy = copiesById.get(copyId);
        if (copy == null) {
            throw new LibraryException("unknown copy " + copyId);
        }
        return copy;
    }
}
