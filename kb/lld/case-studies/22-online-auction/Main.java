import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static final Instant START = Instant.parse("2026-03-01T12:00:00Z");

    public static void main(String[] args) throws Exception {
        biddingRules();
        reserveNotMet();
        scheduledStart();
        autoClose();
        cancellation();
        raceWithoutLock();
        concurrentEqualBids();
        concurrentDistinctBids();
    }

    static void biddingRules() {
        System.out.println("== 1. Bidding rules and winner notification ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        service.addListener(new ConsoleNotifier(clock));
        Auction camera = service.create("sam", "Vintage camera", 10_000, 500, 15_000,
                START, START.plus(Duration.ofHours(1)));
        System.out.println("State " + camera.state() + ", minimum bid "
                + money(camera.minimumNextBid()));
        bid(service, camera, "alice", 10_000);
        bid(service, camera, "bob", 10_200);
        bid(service, camera, "bob", 10_500);
        bid(service, camera, "sam", 20_000);
        bid(service, camera, "carol", 9_000);
        bid(service, camera, "alice", 16_000);
        bid(service, camera, "alice", 16_500);
        System.out.println("Highest bid " + money(camera.highestBid().amountCents())
                + " by " + camera.highestBid().bidderId());
        clock.advance(Duration.ofMinutes(61));
        bid(service, camera, "bob", 17_000);
        System.out.println("State " + camera.state() + ", winner "
                + camera.highestBid().bidderId() + " pays "
                + money(camera.highestBid().amountCents()));
        System.out.println("Accepted bids in order: " + camera.bids().stream()
                .map(b -> b.bidderId() + " " + money(b.amountCents())).toList());
    }

    static void reserveNotMet() {
        System.out.println("== 2. Reserve price not met ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        service.addListener(new ConsoleNotifier(clock));
        Auction watch = service.create("sam", "Pocket watch", 5_000, 500, 20_000,
                START, START.plus(Duration.ofMinutes(30)));
        bid(service, watch, "dina", 12_000);
        clock.advance(Duration.ofMinutes(30));
        System.out.println("Clock at the end time " + clockText(clock.instant()));
        bid(service, watch, "eve", 25_000);
        System.out.println("Closed by the tick after the lazy close: " + service.closeExpired());
        System.out.println("State " + watch.state());
    }

    static void scheduledStart() {
        System.out.println("== 3. Scheduled start ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        Auction lamp = service.create("sam", "Brass lamp", 2_000, 100, 0,
                START.plus(Duration.ofMinutes(30)), START.plus(Duration.ofHours(2)));
        System.out.println("State " + lamp.state());
        bid(service, lamp, "erin", 2_000);
        clock.advance(Duration.ofMinutes(30));
        bid(service, lamp, "erin", 2_000);
        System.out.println("State " + lamp.state());
    }

    static void autoClose() {
        System.out.println("== 4. Auto-close by end time ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        service.addListener(new ConsoleNotifier(clock));
        Auction late = service.create("sam", "Chair", 1_000, 100, 0,
                START, START.plus(Duration.ofMinutes(30)));
        Auction early = service.create("sam", "Table", 1_000, 100, 0,
                START, START.plus(Duration.ofMinutes(10)));
        Auction middle = service.create("sam", "Shelf", 1_000, 100, 0,
                START, START.plus(Duration.ofMinutes(20)));
        bid(service, late, "fay", 1_500);
        bid(service, early, "gus", 1_200);
        bid(service, middle, "hal", 1_100);
        clock.advance(Duration.ofMinutes(25));
        System.out.println("Closed by the first tick: " + service.closeExpired());
        System.out.println("State of the Chair: " + late.state());
        clock.advance(Duration.ofMinutes(5));
        System.out.println("Closed by the second tick: " + service.closeExpired());
        System.out.println("Closed by the third tick: " + service.closeExpired());
    }

    static void cancellation() {
        System.out.println("== 5. Cancellation ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        Auction vase = service.create("sam", "Vase", 1_000, 100, 0,
                START, START.plus(Duration.ofHours(1)));
        System.out.println("Cancel by another user: " + describe(
                () -> service.cancel(vase.id(), "mallory")));
        bid(service, vase, "ivy", 1_000);
        System.out.println("Cancel with a bid: " + describe(
                () -> service.cancel(vase.id(), "sam")));
        Auction rug = service.create("sam", "Rug", 1_000, 100, 0,
                START, START.plus(Duration.ofHours(1)));
        System.out.println("Cancel without a bid: " + describe(
                () -> service.cancel(rug.id(), "sam")));
        bid(service, rug, "ivy", 1_000);
    }

    static void raceWithoutLock() throws Exception {
        System.out.println("== 6. The race without a lock ==");
        UnsafeHighBid unsafe = new UnsafeHighBid();
        CyclicBarrier afterCheck = new CyclicBarrier(2);
        AtomicInteger accepted = new AtomicInteger();
        Runnable bidder = () -> {
            if (unsafe.bid(5_000, afterCheck)) {
                accepted.incrementAndGet();
            }
        };
        Thread a = new Thread(bidder);
        Thread b = new Thread(bidder);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Accepted bids of the same amount: " + accepted.get());
    }

    static void concurrentEqualBids() throws Exception {
        System.out.println("== 7. Ten concurrent bids of the same amount ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        Auction auction = service.create("sam", "Painting", 10_000, 500, 0,
                START, START.plus(Duration.ofHours(1)));
        int[] amounts = new int[10];
        Arrays.fill(amounts, 50_000);
        BidCounts counts = runConcurrentBids(service, auction, amounts);
        System.out.println("Accepted " + counts.accepted + ", rejected " + counts.rejected);
        System.out.println("Highest bid " + money(auction.highestBid().amountCents()));
    }

    static void concurrentDistinctBids() throws Exception {
        System.out.println("== 8. Twenty concurrent bids of different amounts ==");
        ManualClock clock = new ManualClock(START);
        AuctionService service = new AuctionService(clock);
        EventCounter counter = new EventCounter();
        service.addListener(counter);
        Auction auction = service.create("sam", "Piano", 10_000, 500, 0,
                START, START.plus(Duration.ofHours(1)));
        int[] amounts = new int[20];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = 10_000 + 1_000 * (i + 1);
        }
        BidCounts counts = runConcurrentBids(service, auction, amounts);
        List<Bid> history = auction.bids();
        boolean increasing = true;
        for (int i = 1; i < history.size(); i++) {
            increasing &= history.get(i).amountCents() > history.get(i - 1).amountCents();
        }
        System.out.println("Accepted plus rejected: " + (counts.accepted + counts.rejected));
        System.out.println("Highest bid " + money(auction.highestBid().amountCents())
                + " by " + auction.highestBid().bidderId());
        System.out.println("Accepted bids strictly increasing: " + increasing);
        System.out.println("Outbid notices equal accepted bids minus one: "
                + (counter.outbids() == counts.accepted - 1));
    }

    record BidCounts(int accepted, int rejected) { }

    static BidCounts runConcurrentBids(AuctionService service, Auction auction, int[] amounts)
            throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < amounts.length; i++) {
            String bidder = "bidder" + (i + 1);
            long amount = amounts[i];
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                switch (service.placeBid(auction.id(), bidder, amount)) {
                    case Accepted a -> accepted.incrementAndGet();
                    case Rejected r -> rejected.incrementAndGet();
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        return new BidCounts(accepted.get(), rejected.get());
    }

    static void bid(AuctionService service, Auction auction, String bidder, long amount) {
        String text = switch (service.placeBid(auction.id(), bidder, amount)) {
            case Accepted a -> "accepted";
            case Rejected r -> "rejected (" + r.reason() + ")";
        };
        System.out.println(bidder + " bids " + money(amount) + ": " + text);
    }

    static String describe(Runnable action) {
        try {
            action.run();
            return "done";
        } catch (AuctionException e) {
            return "rejected (" + e.getMessage() + ")";
        }
    }

    static String money(long cents) {
        return String.format(Locale.ROOT, "$%d.%02d", cents / 100, cents % 100);
    }

    static String clockText(Instant instant) {
        return LocalTime.ofInstant(instant, ZoneOffset.UTC).toString();
    }
}

class AuctionException extends RuntimeException {
    AuctionException(String message) {
        super(message);
    }
}

/** A clock that only moves when the caller advances it, so the demo is deterministic. */
class ManualClock extends Clock {
    private volatile Instant now;

    ManualClock(Instant start) {
        this.now = start;
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}

record Bid(long sequence, String bidderId, long amountCents, Instant placedAt) { }

sealed interface BidResult permits Accepted, Rejected { }

record Accepted(Bid bid, Bid previousHighest) implements BidResult { }

record Rejected(String reason) implements BidResult { }

enum AuctionState {
    SCHEDULED, OPEN, SOLD, UNSOLD, CANCELLED;

    boolean isFinished() {
        return this == SOLD || this == UNSOLD || this == CANCELLED;
    }
}

/** The result of a close, sent to the listeners once. */
record AuctionOutcome(Auction auction, AuctionState state, Bid winningBid) { }

class Auction {
    private final String id;
    private final String sellerId;
    private final String itemName;
    private final long startingPriceCents;
    private final long minIncrementCents;
    private final long reservePriceCents;
    private final Instant startTime;
    private final Instant endTime;
    private final List<Bid> bids = new ArrayList<>();
    private AuctionState state = AuctionState.SCHEDULED;

    Auction(String id, String sellerId, String itemName, long startingPriceCents,
            long minIncrementCents, long reservePriceCents, Instant startTime, Instant endTime) {
        this.id = id;
        this.sellerId = sellerId;
        this.itemName = itemName;
        this.startingPriceCents = startingPriceCents;
        this.minIncrementCents = minIncrementCents;
        this.reservePriceCents = reservePriceCents;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Moves the auction through its time-based states for the given time.
     * Returns the outcome if this call closed the auction, so exactly one caller sees it.
     */
    synchronized Optional<AuctionOutcome> advance(Instant now) {
        if (state == AuctionState.SCHEDULED && !now.isBefore(startTime)) {
            state = AuctionState.OPEN;
        }
        if (state == AuctionState.OPEN && !now.isBefore(endTime)) {
            Bid winning = highestBid();
            boolean sold = winning != null && winning.amountCents() >= reservePriceCents;
            state = sold ? AuctionState.SOLD : AuctionState.UNSOLD;
            return Optional.of(new AuctionOutcome(this, state, sold ? winning : null));
        }
        return Optional.empty();
    }

    /** Checks all rules and records the bid in one step, under the auction lock. */
    synchronized BidResult placeBid(String bidderId, long amountCents, Instant now) {
        if (state == AuctionState.SCHEDULED) {
            return new Rejected("auction has not started");
        }
        if (state == AuctionState.CANCELLED) {
            return new Rejected("auction was cancelled");
        }
        if (state != AuctionState.OPEN || !now.isBefore(endTime)) {
            return new Rejected("auction has ended");
        }
        if (bidderId.equals(sellerId)) {
            return new Rejected("the seller cannot bid");
        }
        if (amountCents < minimumNextBid()) {
            return new Rejected("minimum bid is " + Main.money(minimumNextBid()));
        }
        Bid previous = highestBid();
        Bid bid = new Bid(bids.size() + 1, bidderId, amountCents, now);
        bids.add(bid);
        return new Accepted(bid, previous);
    }

    synchronized void cancel(String requesterId) {
        if (!requesterId.equals(sellerId)) {
            throw new AuctionException("only the seller can cancel");
        }
        if (state.isFinished()) {
            throw new AuctionException("auction is already " + state);
        }
        if (!bids.isEmpty()) {
            throw new AuctionException("auction already has bids");
        }
        state = AuctionState.CANCELLED;
    }

    synchronized long minimumNextBid() {
        return bids.isEmpty() ? startingPriceCents : highestBid().amountCents() + minIncrementCents;
    }

    synchronized Bid highestBid() {
        return bids.isEmpty() ? null : bids.get(bids.size() - 1);
    }

    synchronized List<Bid> bids() {
        return List.copyOf(bids);
    }

    synchronized AuctionState state() {
        return state;
    }

    String id() {
        return id;
    }

    String sellerId() {
        return sellerId;
    }

    String itemName() {
        return itemName;
    }

    Instant endTime() {
        return endTime;
    }
}

interface AuctionListener {
    void onOutbid(Auction auction, String previousBidderId, Bid newBid);

    void onClosed(AuctionOutcome outcome);
}

class AuctionService {
    private final Clock clock;
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final PriorityQueue<Auction> closingQueue = new PriorityQueue<>(
            Comparator.comparing(Auction::endTime).thenComparing(Auction::id));
    private final List<AuctionListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger();

    AuctionService(Clock clock) {
        this.clock = clock;
    }

    void addListener(AuctionListener listener) {
        listeners.add(listener);
    }

    Auction create(String sellerId, String itemName, long startingPriceCents,
                   long minIncrementCents, long reservePriceCents, Instant start, Instant end) {
        if (startingPriceCents <= 0 || minIncrementCents <= 0 || reservePriceCents < 0) {
            throw new AuctionException("prices must be positive");
        }
        if (!end.isAfter(start)) {
            throw new AuctionException("end time must be after start time");
        }
        Auction auction = new Auction("A-" + sequence.incrementAndGet(), sellerId, itemName,
                startingPriceCents, minIncrementCents, reservePriceCents, start, end);
        auctions.put(auction.id(), auction);
        synchronized (closingQueue) {
            closingQueue.add(auction);
        }
        return auction;
    }

    BidResult placeBid(String auctionId, String bidderId, long amountCents) {
        Auction auction = find(auctionId);
        Instant now = clock.instant();
        auction.advance(now).ifPresent(this::publishClosed);
        BidResult result = auction.placeBid(bidderId, amountCents, now);
        if (result instanceof Accepted accepted && accepted.previousHighest() != null
                && !accepted.previousHighest().bidderId().equals(bidderId)) {
            for (AuctionListener listener : listeners) {
                listener.onOutbid(auction, accepted.previousHighest().bidderId(), accepted.bid());
            }
        }
        return result;
    }

    void cancel(String auctionId, String requesterId) {
        Auction auction = find(auctionId);
        auction.advance(clock.instant()).ifPresent(this::publishClosed);
        auction.cancel(requesterId);
    }

    /** Closes every auction whose end time has passed. Returns how many this call closed. */
    int closeExpired() {
        Instant now = clock.instant();
        List<Auction> due = new ArrayList<>();
        synchronized (closingQueue) {
            while (!closingQueue.isEmpty() && !closingQueue.peek().endTime().isAfter(now)) {
                due.add(closingQueue.poll());
            }
        }
        int closed = 0;
        for (Auction auction : due) {
            Optional<AuctionOutcome> outcome = auction.advance(now);
            outcome.ifPresent(this::publishClosed);
            closed += outcome.isPresent() ? 1 : 0;
        }
        return closed;
    }

    private void publishClosed(AuctionOutcome outcome) {
        for (AuctionListener listener : listeners) {
            listener.onClosed(outcome);
        }
    }

    private Auction find(String auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new AuctionException("unknown auction " + auctionId);
        }
        return auction;
    }
}

/** The wrong form: check, then write, with no lock. A hook forces both threads past the check. */
class UnsafeHighBid {
    private long highest;

    boolean bid(long amount, CyclicBarrier afterCheck) {
        if (amount > highest) {
            try {
                afterCheck.await();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            highest = amount;
            return true;
        }
        return false;
    }
}

class ConsoleNotifier implements AuctionListener {
    private final Clock clock;

    ConsoleNotifier(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void onOutbid(Auction auction, String previousBidderId, Bid newBid) {
        System.out.println("  notify " + previousBidderId + ": outbid on " + auction.itemName()
                + " at " + Main.money(newBid.amountCents()));
    }

    @Override
    public void onClosed(AuctionOutcome outcome) {
        String time = Main.clockText(clock.instant());
        Auction auction = outcome.auction();
        if (outcome.state() == AuctionState.SOLD) {
            System.out.println("  [" + time + "] notify " + outcome.winningBid().bidderId()
                    + ": you won " + auction.itemName() + " for "
                    + Main.money(outcome.winningBid().amountCents()));
            System.out.println("  [" + time + "] notify " + auction.sellerId() + ": "
                    + auction.itemName() + " sold");
        } else {
            System.out.println("  [" + time + "] notify " + auction.sellerId() + ": "
                    + auction.itemName() + " closed without a sale");
        }
    }
}

class EventCounter implements AuctionListener {
    private final AtomicInteger outbids = new AtomicInteger();

    @Override
    public void onOutbid(Auction auction, String previousBidderId, Bid newBid) {
        outbids.incrementAndGet();
    }

    @Override
    public void onClosed(AuctionOutcome outcome) { }

    int outbids() {
        return outbids.get();
    }
}
