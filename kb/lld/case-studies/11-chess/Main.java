import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("== Game 1: rejected moves, then fool's mate ==");
        Game game1 = new Game();
        System.out.print(game1.render());
        System.out.println("Legal moves for WHITE at the start: "
                + game1.allLegalMoves(Color.WHITE).size());
        move(game1, "e7", "e5");
        move(game1, "a1", "a3");
        move(game1, "e2", "e5");
        move(game1, "f2", "f3");
        move(game1, "e7", "e5");
        move(game1, "g2", "g4");
        move(game1, "d8", "h4");
        System.out.println("Status: " + game1.status() + ", winner: "
                + game1.winner().map(Color::name).orElse("none"));
        move(game1, "a2", "a3");
        System.out.println("History: " + String.join(" ", game1.history()));
        System.out.print(game1.render());

        System.out.println();
        System.out.println("== Game 2: check, and moves that leave the king in check ==");
        Game game2 = new Game();
        move(game2, "e2", "e4");
        move(game2, "f7", "f6");
        move(game2, "d1", "h5");
        System.out.println("Status: " + game2.status());
        move(game2, "a7", "a6");
        move(game2, "e8", "f7");
        move(game2, "g7", "g6");
        System.out.println("Status: " + game2.status());

        System.out.println();
        System.out.println("== Game 3: castling ==");
        Game game3 = new Game();
        move(game3, "e1", "g1");
        move(game3, "e2", "e4");
        move(game3, "e7", "e5");
        move(game3, "g1", "f3");
        move(game3, "g8", "f6");
        move(game3, "f1", "c4");
        move(game3, "f8", "c5");
        move(game3, "e1", "g1");
        move(game3, "e8", "g8");
        System.out.println("History: " + String.join(" ", game3.history()));
        System.out.print(game3.render());

        System.out.println("-- A king that moved loses the right to castle");
        Game game3b = new Game();
        for (String[] step : new String[][] {{"e2", "e4"}, {"e7", "e5"}, {"g1", "f3"},
            {"g8", "f6"}, {"f1", "c4"}, {"f8", "c5"}, {"e1", "e2"}, {"b8", "c6"},
            {"e2", "e1"}, {"a7", "a6"}, {"e1", "g1"}}) {
            move(game3b, step[0], step[1]);
        }

        System.out.println("-- A rook captured on its home square ends the right to castle");
        Game game3c = new Game();
        for (String[] step : new String[][] {{"g2", "g4"}, {"b7", "b6"}, {"g1", "f3"},
            {"c8", "b7"}, {"f1", "h3"}, {"b7", "f3"}, {"e2", "e3"}, {"f3", "h1"},
            {"e1", "g1"}}) {
            move(game3c, step[0], step[1]);
        }

        System.out.println();
        System.out.println("== Game 4: en passant and promotion ==");
        Game game4 = new Game();
        move(game4, "e2", "e4");
        move(game4, "a7", "a6");
        move(game4, "e4", "e5");
        move(game4, "d7", "d5");
        move(game4, "e5", "d6");
        move(game4, "g8", "f6");
        move(game4, "d6", "c7");
        move(game4, "b8", "c6");
        move(game4, "c7", "d8");
        move(game4, "e8", "d8");
        System.out.println("History: " + String.join(" ", game4.history()));
        System.out.print(game4.render());

        System.out.println();
        System.out.println("== Game 5: stalemate (Sam Loyd's ten-move line) ==");
        Game game5 = new Game();
        for (String[] step : new String[][] {{"e2", "e3"}, {"a7", "a5"}, {"d1", "h5"},
            {"a8", "a6"}, {"h5", "a5"}, {"h7", "h5"}, {"a5", "c7"}, {"a6", "h6"},
            {"h2", "h4"}, {"f7", "f6"}, {"c7", "d7"}, {"e8", "f7"}, {"d7", "b7"},
            {"d8", "d3"}, {"b7", "b8"}, {"d3", "h7"}, {"b8", "c8"}, {"f7", "g6"},
            {"c8", "e6"}}) {
            move(game5, step[0], step[1]);
        }
        System.out.println("Status: " + game5.status() + ", winner: "
                + game5.winner().map(Color::name).orElse("none"));
        System.out.println("Legal moves for BLACK: " + game5.allLegalMoves(Color.BLACK).size());
        move(game5, "g6", "g5");
        System.out.print(game5.render());

        System.out.println();
        System.out.println("== Game 6: two threads move for WHITE at the same time ==");
        Game race = new Game();
        AtomicInteger accepted = new AtomicInteger();
        Thread first = new Thread(() -> tryMove(race, "e2", "e4", accepted));
        Thread second = new Thread(() -> tryMove(race, "d2", "d4", accepted));
        first.start();
        second.start();
        first.join();
        second.join();
        System.out.println("Moves accepted: " + accepted.get());
        System.out.println("Moves in history: " + race.history().size());
        System.out.println("Next to move: " + race.turn());
    }

    private static void move(Game game, String from, String to) {
        Color side = game.turn();
        try {
            System.out.println(side + ": " + game.play(from, to));
        } catch (IllegalMoveException e) {
            System.out.println("Rejected " + from + "-" + to + ": " + e.getMessage());
        }
    }

    private static void tryMove(Game game, String from, String to, AtomicInteger accepted) {
        try {
            game.play(from, to);
            accepted.incrementAndGet();
        } catch (IllegalMoveException e) {
            // The other thread moved first. This thread's move is not legal any more.
        }
    }
}

enum Color {
    WHITE, BLACK;

    Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    int forward() {
        return this == WHITE ? 1 : -1;
    }

    int homeRow() {
        return this == WHITE ? 0 : 7;
    }
}

enum PieceType {
    KING('K'), QUEEN('Q'), ROOK('R'), BISHOP('B'), KNIGHT('N'), PAWN('P');

    private final char letter;

    PieceType(char letter) {
        this.letter = letter;
    }

    char letter() {
        return letter;
    }
}

record Piece(PieceType type, Color color) {
    char symbol() {
        return color == Color.WHITE ? type.letter() : Character.toLowerCase(type.letter());
    }
}

record Position(int row, int col) {
    Position {
        if (!isValid(row, col)) {
            throw new IllegalArgumentException("Off the board: " + row + "," + col);
        }
    }

    static boolean isValid(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    static Position parse(String square) {
        if (square.length() != 2) {
            throw new IllegalArgumentException("Bad square: " + square);
        }
        return new Position(square.charAt(1) - '1', square.charAt(0) - 'a');
    }

    @Override
    public String toString() {
        return "" + (char) ('a' + col) + (char) ('1' + row);
    }
}

enum MoveKind {
    NORMAL, DOUBLE_PUSH, EN_PASSANT, CASTLE_KING_SIDE, CASTLE_QUEEN_SIDE, PROMOTION
}

record Move(Position from, Position to, MoveKind kind, PieceType promotion) {
    static Move normal(Position from, Position to) {
        return new Move(from, to, MoveKind.NORMAL, null);
    }
}

enum GameStatus {
    ONGOING, CHECK, CHECKMATE, STALEMATE;

    boolean isOver() {
        return this == CHECKMATE || this == STALEMATE;
    }
}

class IllegalMoveException extends RuntimeException {
    IllegalMoveException(String message) {
        super(message);
    }
}

class Board {
    private static final PieceType[] BACK_ROW = {PieceType.ROOK, PieceType.KNIGHT,
        PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
        PieceType.KNIGHT, PieceType.ROOK};

    private final Piece[][] grid = new Piece[8][8];

    static Board initial() {
        Board board = new Board();
        for (int col = 0; col < 8; col++) {
            for (Color color : Color.values()) {
                int back = color.homeRow();
                int pawns = back + color.forward();
                board.grid[back][col] = new Piece(BACK_ROW[col], color);
                board.grid[pawns][col] = new Piece(PieceType.PAWN, color);
            }
        }
        return board;
    }

    Board copy() {
        Board copy = new Board();
        for (int row = 0; row < 8; row++) {
            copy.grid[row] = grid[row].clone();
        }
        return copy;
    }

    Piece at(Position position) {
        return grid[position.row()][position.col()];
    }

    private void put(Position position, Piece piece) {
        grid[position.row()][position.col()] = piece;
    }

    List<Position> positionsOf(Color color) {
        List<Position> result = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.color() == color) {
                    result.add(new Position(row, col));
                }
            }
        }
        return result;
    }

    Optional<Position> findKing(Color color) {
        return positionsOf(color).stream()
                .filter(p -> at(p).type() == PieceType.KING)
                .findFirst();
    }

    // Applies a move that the rules already accepted. Handles the three special moves.
    void apply(Move move) {
        Piece piece = at(move.from());
        int row = move.from().row();
        put(move.from(), null);
        switch (move.kind()) {
            case EN_PASSANT -> put(new Position(row, move.to().col()), null);
            case CASTLE_KING_SIDE -> moveRook(row, 7, 5);
            case CASTLE_QUEEN_SIDE -> moveRook(row, 0, 3);
            case PROMOTION -> piece = new Piece(move.promotion(), piece.color());
            default -> { }
        }
        put(move.to(), piece);
    }

    private void moveRook(int row, int fromCol, int toCol) {
        put(new Position(row, toCol), at(new Position(row, fromCol)));
        put(new Position(row, fromCol), null);
    }

    String render() {
        StringBuilder text = new StringBuilder();
        for (int row = 7; row >= 0; row--) {
            text.append(row + 1).append(' ');
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                text.append(piece == null ? '.' : piece.symbol());
                text.append(col < 7 ? ' ' : '\n');
            }
        }
        return text.append("  a b c d e f g h\n").toString();
    }
}

interface MoveRule {
    // Squares that the piece attacks. A square with a piece of the same color is included,
    // because that piece is defended.
    List<Position> attacks(Board board, Position from);

    // The pseudo-legal moves: the piece obeys its own movement rule, but the move may still
    // leave its own king in check. The Game removes those moves.
    default List<Move> moves(Board board, Position from, Position enPassant) {
        Color mine = board.at(from).color();
        List<Move> result = new ArrayList<>();
        for (Position to : attacks(board, from)) {
            Piece target = board.at(to);
            if (target == null || target.color() != mine) {
                result.add(Move.normal(from, to));
            }
        }
        return result;
    }
}

class SlidingRule implements MoveRule {
    private final int[][] directions;

    SlidingRule(int[][] directions) {
        this.directions = directions;
    }

    @Override
    public List<Position> attacks(Board board, Position from) {
        List<Position> squares = new ArrayList<>();
        for (int[] d : directions) {
            int row = from.row() + d[0];
            int col = from.col() + d[1];
            while (Position.isValid(row, col)) {
                Position square = new Position(row, col);
                squares.add(square);
                if (board.at(square) != null) {
                    break;
                }
                row += d[0];
                col += d[1];
            }
        }
        return squares;
    }
}

class StepRule implements MoveRule {
    private final int[][] offsets;

    StepRule(int[][] offsets) {
        this.offsets = offsets;
    }

    @Override
    public List<Position> attacks(Board board, Position from) {
        List<Position> squares = new ArrayList<>();
        for (int[] d : offsets) {
            int row = from.row() + d[0];
            int col = from.col() + d[1];
            if (Position.isValid(row, col)) {
                squares.add(new Position(row, col));
            }
        }
        return squares;
    }
}

class PawnRule implements MoveRule {
    private static final PieceType[] PROMOTIONS = {PieceType.QUEEN, PieceType.ROOK,
        PieceType.BISHOP, PieceType.KNIGHT};

    @Override
    public List<Position> attacks(Board board, Position from) {
        Color color = board.at(from).color();
        List<Position> squares = new ArrayList<>();
        for (int dCol : new int[] {-1, 1}) {
            int row = from.row() + color.forward();
            int col = from.col() + dCol;
            if (Position.isValid(row, col)) {
                squares.add(new Position(row, col));
            }
        }
        return squares;
    }

    @Override
    public List<Move> moves(Board board, Position from, Position enPassant) {
        Color color = board.at(from).color();
        List<Move> result = new ArrayList<>();
        Position one = new Position(from.row() + color.forward(), from.col());
        if (board.at(one) == null) {
            addMove(result, from, one, color);
            int startRow = color.homeRow() + color.forward();
            Position two = new Position(one.row() + color.forward(), from.col());
            if (from.row() == startRow && board.at(two) == null) {
                result.add(new Move(from, two, MoveKind.DOUBLE_PUSH, null));
            }
        }
        for (Position diagonal : attacks(board, from)) {
            Piece target = board.at(diagonal);
            if (target != null && target.color() != color) {
                addMove(result, from, diagonal, color);
            } else if (target == null && diagonal.equals(enPassant)
                    && isEnemyPawn(board.at(new Position(from.row(), diagonal.col())), color)) {
                result.add(new Move(from, diagonal, MoveKind.EN_PASSANT, null));
            }
        }
        return result;
    }

    // En passant needs the enemy pawn that made the double step beside this pawn.
    private static boolean isEnemyPawn(Piece piece, Color color) {
        return piece != null && piece.type() == PieceType.PAWN && piece.color() != color;
    }

    // A pawn that reaches the last row produces one move for each promotion piece.
    private void addMove(List<Move> result, Position from, Position to, Color color) {
        if (to.row() == color.opposite().homeRow()) {
            for (PieceType type : PROMOTIONS) {
                result.add(new Move(from, to, MoveKind.PROMOTION, type));
            }
        } else {
            result.add(Move.normal(from, to));
        }
    }
}

final class MoveRules {
    private static final int[][] STRAIGHT = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] EIGHT = {{1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] KNIGHT_JUMPS = {{1, 2}, {2, 1}, {-1, 2}, {-2, 1},
        {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};
    private static final Map<PieceType, MoveRule> RULES = new EnumMap<>(PieceType.class);

    static {
        RULES.put(PieceType.ROOK, new SlidingRule(STRAIGHT));
        RULES.put(PieceType.BISHOP, new SlidingRule(DIAGONAL));
        RULES.put(PieceType.QUEEN, new SlidingRule(EIGHT));
        RULES.put(PieceType.KNIGHT, new StepRule(KNIGHT_JUMPS));
        RULES.put(PieceType.KING, new StepRule(EIGHT));
        RULES.put(PieceType.PAWN, new PawnRule());
    }

    private MoveRules() {
    }

    static MoveRule of(PieceType type) {
        return RULES.get(type);
    }

    static boolean isAttacked(Board board, Position square, Color by) {
        for (Position from : board.positionsOf(by)) {
            if (of(board.at(from).type()).attacks(board, from).contains(square)) {
                return true;
            }
        }
        return false;
    }
}

class Game {
    private final Board board = Board.initial();
    private final Set<Position> touched = new HashSet<>();
    private final List<String> history = new ArrayList<>();
    private Color turn = Color.WHITE;
    private Position enPassant;
    private GameStatus status = GameStatus.ONGOING;

    // Plays one move for the side to move. Returns the notation of the move.
    synchronized String play(String from, String to) {
        return play(from, to, null);
    }

    synchronized String play(String from, String to, PieceType promotion) {
        if (status.isOver()) {
            throw new IllegalMoveException("The game is over: " + status);
        }
        Position source = Position.parse(from);
        Position target = Position.parse(to);
        Piece piece = board.at(source);
        if (piece == null) {
            throw new IllegalMoveException("No piece on " + source);
        }
        if (piece.color() != turn) {
            throw new IllegalMoveException("It is " + turn + "'s turn");
        }
        List<Move> candidates = pseudoMoves(source).stream()
                .filter(m -> m.to().equals(target)).toList();
        if (candidates.isEmpty()) {
            throw new IllegalMoveException(piece.type() + " cannot move from " + source
                    + " to " + target);
        }
        PieceType wanted = promotion == null ? PieceType.QUEEN : promotion;
        Move move = candidates.stream()
                .filter(m -> m.promotion() == null || m.promotion() == wanted)
                .findFirst()
                .orElseThrow(() -> new IllegalMoveException("Cannot promote to " + wanted));
        if (!isSafe(move, turn)) {
            throw new IllegalMoveException("The move leaves the king in check");
        }
        String text = notation(piece, move, board.at(target) != null);
        board.apply(move);
        touched.add(source);
        touched.add(target);
        enPassant = move.kind() == MoveKind.DOUBLE_PUSH
                ? new Position((source.row() + target.row()) / 2, source.col()) : null;
        turn = turn.opposite();
        status = computeStatus();
        text += status == GameStatus.CHECKMATE ? "#" : status == GameStatus.CHECK ? "+" : "";
        history.add(text);
        return text;
    }

    synchronized List<Move> legalMoves(Position from) {
        Piece piece = board.at(from);
        if (piece == null) {
            return List.of();
        }
        return pseudoMoves(from).stream().filter(m -> isSafe(m, piece.color())).toList();
    }

    synchronized List<Move> allLegalMoves(Color color) {
        List<Move> result = new ArrayList<>();
        for (Position from : board.positionsOf(color)) {
            for (Move move : pseudoMoves(from)) {
                if (isSafe(move, color)) {
                    result.add(move);
                }
            }
        }
        return result;
    }

    synchronized Color turn() {
        return turn;
    }

    synchronized GameStatus status() {
        return status;
    }

    synchronized Optional<Color> winner() {
        return status == GameStatus.CHECKMATE ? Optional.of(turn.opposite()) : Optional.empty();
    }

    synchronized List<String> history() {
        return List.copyOf(history);
    }

    synchronized String render() {
        return board.render();
    }

    private List<Move> pseudoMoves(Position from) {
        Piece piece = board.at(from);
        List<Move> moves = new ArrayList<>(
                MoveRules.of(piece.type()).moves(board, from, enPassant));
        if (piece.type() == PieceType.KING) {
            moves.addAll(castlingMoves(piece.color()));
        }
        return moves;
    }

    // A square that no move has left or entered still holds its starting piece. So an
    // untouched king square and an untouched rook square mean that neither piece has moved.
    private List<Move> castlingMoves(Color color) {
        List<Move> result = new ArrayList<>();
        int row = color.homeRow();
        Position king = new Position(row, 4);
        if (touched.contains(king) || MoveRules.isAttacked(board, king, color.opposite())) {
            return result;
        }
        if (canCastle(color, 7, new int[] {5, 6}, new int[] {5, 6})) {
            result.add(new Move(king, new Position(row, 6), MoveKind.CASTLE_KING_SIDE, null));
        }
        if (canCastle(color, 0, new int[] {1, 2, 3}, new int[] {2, 3})) {
            result.add(new Move(king, new Position(row, 2), MoveKind.CASTLE_QUEEN_SIDE, null));
        }
        return result;
    }

    private boolean canCastle(Color color, int rookCol, int[] emptyCols, int[] safeCols) {
        int row = color.homeRow();
        if (touched.contains(new Position(row, rookCol))) {
            return false;
        }
        for (int col : emptyCols) {
            if (board.at(new Position(row, col)) != null) {
                return false;
            }
        }
        for (int col : safeCols) {
            if (MoveRules.isAttacked(board, new Position(row, col), color.opposite())) {
                return false;
            }
        }
        return true;
    }

    // Plays the move on a copy of the board and checks that the king is not attacked.
    private boolean isSafe(Move move, Color color) {
        Board copy = board.copy();
        copy.apply(move);
        Position king = copy.findKing(color).orElseThrow();
        return !MoveRules.isAttacked(copy, king, color.opposite());
    }

    private GameStatus computeStatus() {
        Position king = board.findKing(turn).orElseThrow();
        boolean inCheck = MoveRules.isAttacked(board, king, turn.opposite());
        boolean canMove = !allLegalMoves(turn).isEmpty();
        if (inCheck) {
            return canMove ? GameStatus.CHECK : GameStatus.CHECKMATE;
        }
        return canMove ? GameStatus.ONGOING : GameStatus.STALEMATE;
    }

    private String notation(Piece piece, Move move, boolean capture) {
        if (move.kind() == MoveKind.CASTLE_KING_SIDE) {
            return "O-O";
        }
        if (move.kind() == MoveKind.CASTLE_QUEEN_SIDE) {
            return "O-O-O";
        }
        String letter = piece.type() == PieceType.PAWN ? "" : "" + piece.type().letter();
        boolean takes = capture || move.kind() == MoveKind.EN_PASSANT;
        String text = letter + move.from() + (takes ? "x" : "-") + move.to();
        if (move.kind() == MoveKind.PROMOTION) {
            text += "=" + move.promotion().letter();
        }
        if (move.kind() == MoveKind.EN_PASSANT) {
            text += "(e.p.)";
        }
        return text;
    }
}
