import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        scriptedGame();
        extraTurnGame();
        invalidBoards();
        seededGame();
    }

    static Board smallBoard() {
        return new Board(20, List.of(
                new Jump(3, 11), new Jump(6, 15),
                new Jump(17, 4), new Jump(14, 7)));
    }

    static void scriptedGame() {
        System.out.println("== 1. Scripted dice, exact finish ==");
        Dice dice = new ScriptedDice(6, 3, 4, 3, 2, 6, 2, 6, 5, 3, 6, 1);
        Game game = new Game(smallBoard(), dice, Rules.STANDARD, List.of("Asha", "Ben"));
        while (!game.isOver()) {
            System.out.println(game.playTurn());
        }
        System.out.println("Winner: " + game.winner().name());
    }

    static void extraTurnGame() {
        System.out.println("== 2. Extra turn on the highest roll ==");
        Dice dice = new ScriptedDice(6, 6, 4, 1, 1);
        Game game = new Game(smallBoard(), dice, Rules.EXTRA_TURN_ON_MAX, List.of("Asha", "Ben"));
        while (!game.isOver()) {
            System.out.println(game.playTurn());
        }
        System.out.println("Winner: " + game.winner().name());
    }

    static void invalidBoards() {
        System.out.println("== 3. Invalid board setups ==");
        List<List<Jump>> bad = List.of(
                List.of(new Jump(5, 5)),
                List.of(new Jump(4, 9), new Jump(4, 2)),
                List.of(new Jump(4, 9), new Jump(9, 2)),
                List.of(new Jump(20, 3)));
        for (List<Jump> jumps : bad) {
            try {
                new Board(20, jumps);
            } catch (IllegalArgumentException e) {
                System.out.println("Rejected: " + e.getMessage());
            }
        }
    }

    static void seededGame() {
        System.out.println("== 4. Seeded dice on a 100-square board ==");
        Board board = new Board(100, List.of(
                new Jump(4, 14), new Jump(9, 31), new Jump(20, 38), new Jump(28, 84),
                new Jump(40, 59), new Jump(51, 67), new Jump(63, 81), new Jump(71, 91),
                new Jump(17, 7), new Jump(54, 34), new Jump(62, 19), new Jump(64, 60),
                new Jump(87, 24), new Jump(93, 73), new Jump(95, 75), new Jump(99, 78)));
        Dice dice = new RandomDice(new Random(7), 1, 6);
        Game game = new Game(board, dice, Rules.STANDARD, List.of("Asha", "Ben", "Chen"));
        int shown = 0;
        while (!game.isOver()) {
            TurnResult result = game.playTurn();
            if (shown < 8) {
                System.out.println(result);
                shown++;
            }
        }
        System.out.println("Turns played: " + game.turnCount());
        System.out.println("Winner: " + game.winner().name());
        for (Player p : game.players()) {
            System.out.println(p.name() + " ends on square " + p.position());
        }
    }
}

enum JumpKind { SNAKE, LADDER }

record Jump(int from, int to) {
    JumpKind kind() {
        return to > from ? JumpKind.LADDER : JumpKind.SNAKE;
    }
}

record Rules(boolean exactFinish, boolean extraTurnOnMaxRoll) {
    static final Rules STANDARD = new Rules(true, false);
    static final Rules EXTRA_TURN_ON_MAX = new Rules(true, true);
}

interface Dice {
    int roll();

    int maxRoll();
}

class RandomDice implements Dice {
    private final Random random;
    private final int count;
    private final int faces;

    RandomDice(Random random, int count, int faces) {
        this.random = random;
        this.count = count;
        this.faces = faces;
    }

    @Override
    public int roll() {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(faces) + 1;
        }
        return total;
    }

    @Override
    public int maxRoll() {
        return count * faces;
    }
}

class ScriptedDice implements Dice {
    private final int maxRoll;
    private final Deque<Integer> values = new ArrayDeque<>();

    ScriptedDice(int maxRoll, int... script) {
        this.maxRoll = maxRoll;
        for (int value : script) {
            values.addLast(value);
        }
    }

    @Override
    public int roll() {
        return values.removeFirst();
    }

    @Override
    public int maxRoll() {
        return maxRoll;
    }
}

class Player {
    private final String name;
    private int position;

    Player(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }

    int position() {
        return position;
    }

    void moveTo(int square) {
        this.position = square;
    }
}

class Board {
    private final int lastSquare;
    private final Map<Integer, Jump> jumpsByStart = new HashMap<>();

    Board(int lastSquare, List<Jump> jumps) {
        if (lastSquare < 2) {
            throw new IllegalArgumentException("Board needs at least 2 squares");
        }
        this.lastSquare = lastSquare;
        for (Jump jump : jumps) {
            validate(jump);
            if (jumpsByStart.put(jump.from(), jump) != null) {
                throw new IllegalArgumentException("Two jumps start on square " + jump.from());
            }
        }
        for (Jump jump : jumps) {
            if (jumpsByStart.containsKey(jump.to())) {
                throw new IllegalArgumentException(
                        "Jump " + jump.from() + "->" + jump.to() + " ends where another starts");
            }
        }
    }

    private void validate(Jump jump) {
        if (jump.from() == jump.to()) {
            throw new IllegalArgumentException("A jump must change the square: " + jump.from());
        }
        if (jump.from() < 1 || jump.from() >= lastSquare) {
            throw new IllegalArgumentException("A jump cannot start on square " + jump.from()
                    + " (valid: 1.." + (lastSquare - 1) + ")");
        }
        if (jump.to() < 1 || jump.to() > lastSquare) {
            throw new IllegalArgumentException("A jump ends outside the board: " + jump.to());
        }
    }

    int lastSquare() {
        return lastSquare;
    }

    // Returns the square after a snake or ladder, or the same square if there is none.
    int resolve(int square) {
        Jump jump = jumpsByStart.get(square);
        return jump == null ? square : jump.to();
    }

    Jump jumpAt(int square) {
        return jumpsByStart.get(square);
    }
}

record TurnResult(String player, int roll, int from, int to, int end, String note) {
    @Override
    public String toString() {
        String text = player + " rolls " + roll + ": " + from + " -> " + to;
        if (end != to) {
            text += " -> " + end;
        }
        return note.isEmpty() ? text : text + " (" + note + ")";
    }
}

class Game {
    private final Board board;
    private final Dice dice;
    private final Rules rules;
    private final List<Player> players = new ArrayList<>();
    private final Deque<Player> queue = new ArrayDeque<>();
    private Player winner;
    private int turnCount;

    Game(Board board, Dice dice, Rules rules, List<String> names) {
        if (names.size() < 2) {
            throw new IllegalArgumentException("At least 2 players are needed");
        }
        this.board = board;
        this.dice = dice;
        this.rules = rules;
        for (String name : names) {
            Player player = new Player(name);
            players.add(player);
            queue.addLast(player);
        }
    }

    synchronized boolean isOver() {
        return winner != null;
    }

    synchronized Player winner() {
        return winner;
    }

    synchronized int turnCount() {
        return turnCount;
    }

    synchronized List<Player> players() {
        return List.copyOf(players);
    }

    synchronized TurnResult playTurn() {
        if (isOver()) {
            throw new IllegalStateException("The game is over");
        }
        Player player = queue.pollFirst();
        int roll = dice.roll();
        turnCount++;
        int from = player.position();
        int target = from + roll;
        String note = "";
        if (target > board.lastSquare()) {
            if (rules.exactFinish()) {
                target = from;
                note = "needs exact roll, stays";
            } else {
                target = board.lastSquare();
            }
        }
        int end = board.resolve(target);
        Jump jump = board.jumpAt(target);
        if (jump != null) {
            note = jump.kind().name().toLowerCase(Locale.ROOT);
        }
        player.moveTo(end);
        boolean extraTurn = rules.extraTurnOnMaxRoll() && roll == dice.maxRoll();
        if (end == board.lastSquare()) {
            winner = player;
            note = note.isEmpty() ? "wins" : note + ", wins";
        } else if (extraTurn) {
            queue.addFirst(player);
            note = note.isEmpty() ? "extra turn" : note + ", extra turn";
        } else {
            queue.addLast(player);
        }
        return new TurnResult(player.name(), roll, from, target, end, note);
    }
}
