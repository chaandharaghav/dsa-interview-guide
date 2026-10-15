import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        scriptedWin();
        invalidMoves();
        undoDemo();
        draw();
        fourByFour();
        aiDemo();
    }

    static Game newGame(int size, MoveStrategy x, MoveStrategy o) {
        return new Game(size,
                new Player("Alice", Mark.X, x),
                new Player("Bob", Mark.O, o));
    }

    static void scriptedWin() {
        System.out.println("== 1. Scripted win on a 3x3 board ==");
        Game game = newGame(3,
                new ScriptedStrategy(new Position(0, 0), new Position(1, 1), new Position(2, 2)),
                new ScriptedStrategy(new Position(0, 1), new Position(0, 2)));
        while (game.status() == GameStatus.IN_PROGRESS) {
            Player mover = game.currentPlayer();
            Position p = game.playNextTurn();
            System.out.println(mover.name() + " (" + mover.mark() + ") plays " + p);
        }
        System.out.print(game.board().render());
        System.out.println("Status: " + game.status() + ", winner: " + game.winner().name());
    }

    static void invalidMoves() {
        System.out.println("== 2. Invalid moves ==");
        Game game = newGame(3, null, null);
        game.play(new Position(1, 1));
        String[] labels = {"occupied cell", "outside the board"};
        Position[] attempts = {new Position(1, 1), new Position(3, 0)};
        for (int i = 0; i < attempts.length; i++) {
            try {
                game.play(attempts[i]);
            } catch (IllegalMoveException e) {
                System.out.println("Rejected (" + labels[i] + "): " + e.getMessage());
            }
        }
        System.out.println("Current player after rejects: " + game.currentPlayer().name());
        Game over = newGame(3, null, null);
        int[][] script = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {0, 2}};
        for (int[] move : script) {
            over.play(new Position(move[0], move[1]));
        }
        try {
            over.play(new Position(2, 2));
        } catch (IllegalMoveException e) {
            System.out.println("Rejected (game over): " + e.getMessage());
        }
    }

    static void undoDemo() {
        System.out.println("== 3. Undo ==");
        Game game = newGame(3, null, null);
        game.play(new Position(0, 0));
        game.play(new Position(1, 1));
        game.undo();
        System.out.println("After one undo, next player: " + game.currentPlayer().name());
        game.play(new Position(0, 2));
        game.play(new Position(0, 1));
        game.play(new Position(2, 2));
        game.play(new Position(1, 0));
        game.play(new Position(1, 2));
        System.out.println("Before undo: " + game.status() + ", winner: " + game.winner().name());
        game.undo();
        System.out.println("After undo of the winning move: " + game.status()
                + ", next player: " + game.currentPlayer().name());
        System.out.print(game.board().render());
    }

    static void draw() {
        System.out.println("== 4. Draw ==");
        Game game = newGame(3, null, null);
        int[][] script = {{0, 0}, {0, 1}, {0, 2}, {1, 1}, {1, 0}, {1, 2}, {2, 1}, {2, 0}, {2, 2}};
        for (int[] move : script) {
            game.play(new Position(move[0], move[1]));
        }
        System.out.print(game.board().render());
        System.out.println("Status: " + game.status());
    }

    static void fourByFour() {
        System.out.println("== 5. 4x4 board, win on a column ==");
        Game game = newGame(4, null, null);
        int[][] script = {{0, 0}, {0, 1}, {1, 0}, {1, 1}, {2, 0}, {2, 1}, {3, 0}};
        for (int[] move : script) {
            game.play(new Position(move[0], move[1]));
        }
        System.out.print(game.board().render());
        System.out.println("Status: " + game.status() + ", winner: " + game.winner().name());
    }

    static void aiDemo() {
        System.out.println("== 6. Heuristic AI as Bob ==");
        Game game = newGame(3,
                new ScriptedStrategy(new Position(0, 0), new Position(0, 1), new Position(2, 0),
                        new Position(2, 2)),
                new HeuristicStrategy());
        while (game.status() == GameStatus.IN_PROGRESS) {
            Player mover = game.currentPlayer();
            Position p = game.playNextTurn();
            System.out.println(mover.name() + " (" + mover.mark() + ") plays " + p);
        }
        System.out.print(game.board().render());
        System.out.println("Status: " + game.status() + ", winner: " + game.winner().name());
    }
}

enum Mark {
    X, O;

    char symbol() {
        return this == X ? 'X' : 'O';
    }
}

enum GameStatus { IN_PROGRESS, WON, DRAW }

record Position(int row, int col) {
    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}

class IllegalMoveException extends RuntimeException {
    IllegalMoveException(String message) {
        super(message);
    }
}

interface MoveStrategy {
    // Receives a copy of the board, so the strategy cannot change the game.
    Position chooseMove(Board board, Mark self);
}

record Player(String name, Mark mark, MoveStrategy strategy) { }

class ScriptedStrategy implements MoveStrategy {
    private final Deque<Position> moves = new ArrayDeque<>();

    ScriptedStrategy(Position... script) {
        moves.addAll(List.of(script));
    }

    @Override
    public Position chooseMove(Board board, Mark self) {
        return moves.removeFirst();
    }
}

class HeuristicStrategy implements MoveStrategy {
    @Override
    public Position chooseMove(Board board, Mark self) {
        Mark opponent = self == Mark.X ? Mark.O : Mark.X;
        List<Position> empty = board.emptyCells();
        for (Position p : empty) {
            if (board.wouldWin(self, p)) {
                return p;
            }
        }
        for (Position p : empty) {
            if (board.wouldWin(opponent, p)) {
                return p;
            }
        }
        Position center = new Position(board.size() / 2, board.size() / 2);
        if (empty.contains(center)) {
            return center;
        }
        return empty.get(0);
    }
}

class LineCounts {
    private final int[] rows;
    private final int[] cols;
    private int diagonal;
    private int antiDiagonal;
    private final int size;

    LineCounts(int size) {
        this.size = size;
        this.rows = new int[size];
        this.cols = new int[size];
    }

    void add(Position p, int delta) {
        rows[p.row()] += delta;
        cols[p.col()] += delta;
        if (p.row() == p.col()) {
            diagonal += delta;
        }
        if (p.row() + p.col() == size - 1) {
            antiDiagonal += delta;
        }
    }

    boolean completesLine(Position p) {
        return rows[p.row()] + 1 == size
                || cols[p.col()] + 1 == size
                || (p.row() == p.col() && diagonal + 1 == size)
                || (p.row() + p.col() == size - 1 && antiDiagonal + 1 == size);
    }

    boolean hasFullLine(Position p) {
        return rows[p.row()] == size
                || cols[p.col()] == size
                || (p.row() == p.col() && diagonal == size)
                || (p.row() + p.col() == size - 1 && antiDiagonal == size);
    }
}

class Board {
    private final int size;
    private final Mark[][] cells;
    private final Map<Mark, LineCounts> counts = new EnumMap<>(Mark.class);
    private int filled;

    Board(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Board size must be at least 2");
        }
        this.size = size;
        this.cells = new Mark[size][size];
        for (Mark mark : Mark.values()) {
            counts.put(mark, new LineCounts(size));
        }
    }

    int size() {
        return size;
    }

    boolean isFull() {
        return filled == size * size;
    }

    Mark markAt(Position p) {
        return cells[p.row()][p.col()];
    }

    boolean contains(Position p) {
        return p.row() >= 0 && p.row() < size && p.col() >= 0 && p.col() < size;
    }

    // Places a mark and returns true if the move completes a line. O(1).
    boolean place(Position p, Mark mark) {
        if (!contains(p)) {
            throw new IllegalMoveException("Position " + p + " is outside the board");
        }
        if (cells[p.row()][p.col()] != null) {
            throw new IllegalMoveException("Cell " + p + " is already taken");
        }
        cells[p.row()][p.col()] = mark;
        filled++;
        LineCounts own = counts.get(mark);
        own.add(p, 1);
        return own.hasFullLine(p);
    }

    // Reverses place(). O(1).
    void remove(Position p) {
        Mark mark = cells[p.row()][p.col()];
        cells[p.row()][p.col()] = null;
        filled--;
        counts.get(mark).add(p, -1);
    }

    boolean wouldWin(Mark mark, Position p) {
        return counts.get(mark).completesLine(p);
    }

    List<Position> emptyCells() {
        List<Position> result = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (cells[r][c] == null) {
                    result.add(new Position(r, c));
                }
            }
        }
        return result;
    }

    // Returns an independent board with the same marks and counters. O(N^2).
    Board copy() {
        Board copy = new Board(size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (cells[r][c] != null) {
                    copy.place(new Position(r, c), cells[r][c]);
                }
            }
        }
        return copy;
    }

    String render() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                sb.append(cells[r][c] == null ? '.' : cells[r][c].symbol());
                if (c < size - 1) {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}

class Game {
    private final Board board;
    private final List<Player> players;
    private final Deque<Position> history = new ArrayDeque<>();
    private int turn;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Player winner;

    Game(int size, Player first, Player second) {
        if (first.mark() == second.mark()) {
            throw new IllegalArgumentException("Players need different marks");
        }
        this.board = new Board(size);
        this.players = List.of(first, second);
    }

    // Returns a snapshot. A change to the snapshot does not change the game.
    synchronized Board board() {
        return board.copy();
    }

    synchronized GameStatus status() {
        return status;
    }

    synchronized Player winner() {
        return winner;
    }

    synchronized Player currentPlayer() {
        return players.get(turn);
    }

    // Asks the current player's strategy for a move, then plays it.
    synchronized Position playNextTurn() {
        Player player = currentPlayer();
        Position p = player.strategy().chooseMove(board.copy(), player.mark());
        play(p);
        return p;
    }

    synchronized void play(Position p) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalMoveException("The game is over");
        }
        Player player = players.get(turn);
        boolean won = board.place(p, player.mark());
        history.push(p);
        if (won) {
            status = GameStatus.WON;
            winner = player;
        } else if (board.isFull()) {
            status = GameStatus.DRAW;
        } else {
            turn = (turn + 1) % players.size();
        }
    }

    synchronized void undo() {
        if (history.isEmpty()) {
            throw new IllegalMoveException("Nothing to undo");
        }
        board.remove(history.pop());
        if (status == GameStatus.IN_PROGRESS) {
            turn = (turn - 1 + players.size()) % players.size();
        }
        status = GameStatus.IN_PROGRESS;
        winner = null;
    }
}
