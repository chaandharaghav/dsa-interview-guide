---
priority: P1
---
# Pattern 25: Matrix & Simulation

## 1. Pattern Name
**Matrix Traversal & Manipulation** (traversal orders, rotation, in-place marking, sorted-matrix search)
and **Simulation** (step-by-step state machines, direction arrays, cycle detection in states)

A **matrix** (also called a grid) is a two-dimensional array. In Java it is an `int[][]`: an array
of row arrays. `m` is the number of rows. `n` is the number of columns. Cell `(r, c)` is in row
`r` and column `c`. A **simulation** problem gives you a set of rules and asks for the state
after some steps. You run the rules exactly as written, and you make the run cheap enough.

This pattern has no clever algorithm behind it. The difficulty is precision: the right index at
the right moment, the right boundary, the right order of updates. Interviewers use these
problems to test whether you can write a small, correct program under pressure.

Related pages:

- [Arrays, strings, and linked lists](../foundations/02-arrays-strings-linked-lists.md) explains
  how Java stores a 2D array.
- [Graph representations](../foundations/07-graph-representations.md) explains why a grid is an
  implicit graph.
- [Graph Traversal (Pattern 11)](../11-graph-traversal/README.md) covers BFS and DFS on a grid.
  This page covers the problems that need no graph search.

---

## 2. Core Intuition

### What Interviewers Expect You to Think:

**Key Insight 1: every cell has coordinates, and every order is a formula.**
A traversal order is a rule that maps a step number to a cell. Row order is `(k / n, k % n)`.
Column order swaps the loops. A diagonal is the set of cells where `r - c` is constant. An
anti-diagonal is the set of cells where `r + c` is constant. If you know the formula, you do not
need special cases.

**Key Insight 2: a direction array replaces a pile of `if` statements.**
Store the four moves as data: `dr = {-1, 0, 1, 0}` and `dc = {0, 1, 0, -1}`. A loop over the
index `d` visits all neighbors. Turning right is `d = (d + 1) % 4`. The same idea drives spiral
traversal, robot simulation, and every grid BFS.

**Key Insight 3: in-place work needs an encoding.**
If you overwrite a cell, you lose its old value, and later cells may still need it. You solve
this in one of three ways:

1. Keep the old value in extra bits or an extra marker (Game of Life).
2. Reuse a part of the matrix as scratch space (Set Matrix Zeroes uses the first row and column).
3. Choose an order of updates that never reads a cell after it changes (rotation by layers).

**Key Insight 4: sorted structure gives a search order.**
If rows and columns are sorted, one corner of the matrix is a pivot. At the top-right corner,
every move discards a full row or a full column. This turns an O(m * n) scan into O(m + n).

**Key Insight 5: a simulation that repeats a state repeats forever.**
If the state space is finite and the rules are deterministic, a state that appears twice starts
a cycle. You find the cycle once, then skip whole cycles with the modulo operator.

### Mental Model

Every solution on this page has three parts:

1. **The state:** the current cell `(r, c)`, the direction index `d`, and any marks in the matrix.
2. **The step rule:** how one step changes the state. A direction array gives the next cell. A
   boundary check or a visited mark decides when the direction changes.
3. **The loop:** it applies the step rule until every cell is visited or the step count ends.

You do not need to plan the whole path in advance. You need one correct step rule and a loop
that applies it.

---

## 3. Recognition Signals

### Keywords in Problem Statements:
- "Rotate the image / matrix by 90 degrees", often with "in place" (LC 48 requires it)
- "Spiral order", "zigzag", "diagonal order", "anti-diagonal"
- "Set the entire row and column to ..." (marking with O(1) extra space)
- "Search in a matrix where each row and column is sorted"
- "Next state of the board", "simulate for k steps", "after n days"
- "Robot", "instructions", "commands", "obstacles", "walk"
- "Do it in place", "use O(1) extra space"

### Constraint Hints:
- `m, n <= 200` (LC 73) or smaller (LC 54: `m, n <= 10`): an O(m * n) or O(m * n * 8)
  solution is expected.
- `k` or `n` up to 10^9 days or steps, with a small state: look for a cycle.
- "In place" or "constant extra space": the problem wants an encoding or a scratch row.
- A matrix that is sorted by rows and columns: the problem wants O(m + n) or O(log(m * n)).

### Input/Output Hints:
- Input: `int[][]`, `char[][]`, a list of commands, or an array that you must reshape.
- Output: a list in a special order, the same matrix changed in place, or a final position.

### RED FLAGS - NOT This Pattern:
- "Number of islands", "shortest path in a grid", "can reach the border": the grid is a graph.
  Use [Graph Traversal](../11-graph-traversal/README.md) or
  [Shortest Paths](../18-shortest-paths/README.md).
- "Count paths from the top-left to the bottom-right", "minimum path sum": the grid is a DP
  table. Use [Dynamic Programming](../15-dynamic-programming/README.md).
- "Sum of a sub-rectangle for many queries": use the 2D prefix sum in
  [Prefix Sum](../04-prefix-sum/README.md).
- "Place N queens", "solve the Sudoku": use
  [Backtracking](../08-recursion-backtracking/README.md).

---

## 4. Brute Force Thought Process

### Brute force for in-place problems: copy the matrix.

```text
Problem: rotate an n x n matrix by 90 degrees clockwise.

Brute force thinking:
- Create a new n x n matrix.
- For each cell (r, c), write its value to (c, n - 1 - r) in the new matrix.
- Copy the new matrix back.

Time: O(n^2)   Space: O(n^2)
```

The time is already optimal, because you must touch every cell. The only cost is space. The
copy breaks the in-place requirement of LC 48, so expect the follow-up: "Can you do it in
place?" Say the copy solution
first. It proves that you know the index formula. Then optimize.

### Brute force for Set Matrix Zeroes: mark with a second matrix.

```text
Problem: if a cell is 0, set its whole row and column to 0.

Brute force thinking:
- Copy the matrix.
- For each 0 in the original, zero the row and column in the copy.

Time: O(m * n * (m + n))   Space: O(m * n)
```

A cheaper version keeps two arrays, `zeroRow[m]` and `zeroCol[n]`. It runs in O(m * n) time and
O(m + n) space. The optimal version uses O(1) space (section 6).

### Brute force for a sorted-matrix search: scan every cell.

```text
Time: O(m * n)   Space: O(1)
```

This ignores the sorted order. The interviewer expects you to use it.

### Brute force for a long simulation: run every step.

```text
Problem: simulate a board for n = 10^9 days.

Brute force thinking: run the step function n times.
Time: O(n * cost of one step)
```

If the state space is small, 10^9 steps do not fit in the time limit. The state must repeat, so
find the cycle.

---

## 5. Optimization Insight (The "Aha" Moment)

### Rotation: a rotation is a transpose plus a reflection.

```text
Clockwise rotation of n x n:  (r, c)  ->  (c, n - 1 - r)

Step 1: transpose           (r, c) -> (c, r)       swap across the main diagonal
Step 2: reverse each row    (c, r) -> (c, n-1-r)   mirror left to right

Original      Transpose     Reverse each row (= rotated clockwise)
 1 2 3         1 4 7          7 4 1
 4 5 6   ->    2 5 8    ->    8 5 2
 7 8 9         3 6 9          9 6 3
```

Each step works in place, and each step is a simple loop. The two steps together send every cell
to its rotated position. For a counterclockwise rotation, reverse each row first, then transpose
(or transpose, then reverse each column).

### Set Matrix Zeroes: the matrix stores its own marks.

The first row can record "this column has a zero". The first column can record "this row has a
zero". Two booleans remember whether the first row and the first column had a zero on their own.

### Game of Life: one cell stores two states.

Each cell holds 0 or 1. Bit 0 is the current state. Bit 1 is the next state. A reader that
needs the current state of a neighbor uses `value & 1`, which ignores bit 1. A final pass shifts
every cell right by one bit.

### Sorted matrix: the corner is a decision point.

```text
Rows sorted left to right. Columns sorted top to bottom. Search for target = 5.

   1   4   7  11  15
   2   5   8  12  19
   3   6   9  16  22
  10  13  14  17  24
  18  21  23  26  30

Start at the top-right corner (r = 0, c = 4), the cell with value 15.
Step 1: (0, 4) = 15 > 5. Every cell below 15 in column 4 is larger. Drop the column. Go left.
Step 2: (0, 3) = 11 > 5. Drop column 3. Go left.
Step 3: (0, 2) = 7 > 5. Drop column 2. Go left.
Step 4: (0, 1) = 4 < 5. Every cell left of 4 in row 0 is smaller. Drop the row. Go down.
Step 5: (1, 1) = 5 == 5. Found.

Each step removes one row or one column, so there are at most m + n steps.
```

### Simulation: a repeated state is a cycle.

If the state after step `i` equals the state after step `j` (with `i < j`), then the sequence
repeats with period `j - i`. You skip `(n - i) / (j - i)` full periods, and only simulate the
rest.

---

## 6. Generalized Templates

All templates compile under `javac --release 21`. Each block holds static methods. Put them in
any class.

Assumption: the matrix has at least one row and one column (`m >= 1` and `n >= 1`). The
LeetCode constraints of the matrix problems in section 8 (LC 867, 54, 48, 73, 289, 74, 240, 498)
guarantee it. The templates read `a[0].length`, and that expression throws
`ArrayIndexOutOfBoundsException` if `m = 0`. If your input can be empty, test it first with a
helper and return early, for example `if (isEmpty(a)) return new ArrayList<>();`:

```java
static boolean isEmpty(int[][] a) {
    return a.length == 0 || a[0].length == 0;
}
```

### Template 1: Direction arrays and bounds check

```java
// Order: up, right, down, left. Turning right is d = (d + 1) % 4.
static final int[] DR = {-1, 0, 1, 0};
static final int[] DC = {0, 1, 0, -1};

static boolean inBounds(int r, int c, int rows, int cols) {
    return r >= 0 && r < rows && c >= 0 && c < cols;
}

// Count the live neighbors of a cell. The eight-direction version uses two nested loops.
static int countNeighbors4(int[][] grid, int r, int c) {
    int count = 0;
    for (int d = 0; d < 4; d++) {
        int nr = r + DR[d];
        int nc = c + DC[d];
        if (inBounds(nr, nc, grid.length, grid[0].length)) {
            count += grid[nr][nc];
        }
    }
    return count;
}
```

Use the same order of directions (up, right, down, left) in every problem. Clockwise order makes
a right turn `(d + 1) % 4` and a left turn `(d + 3) % 4`. The expression `(d - 1) % 4` is wrong,
because `%` in Java returns a negative result for a negative left operand.

### Template 2: Traversal orders

```java
// Row by row, left to right. One loop over k replaces two loops.
static int[] rowMajor(int[][] a) {
    int m = a.length, n = a[0].length;
    int[] out = new int[m * n];
    for (int k = 0; k < m * n; k++) {
        out[k] = a[k / n][k % n];
    }
    return out;
}

// Column by column, top to bottom. Swap the loop order.
static int[] columnMajor(int[][] a) {
    int m = a.length, n = a[0].length;
    int[] out = new int[m * n];
    int k = 0;
    for (int c = 0; c < n; c++) {
        for (int r = 0; r < m; r++) {
            out[k++] = a[r][c];
        }
    }
    return out;
}

// Snake order: row by row, and every odd row runs right to left.
static int[] snakeOrder(int[][] a) {
    int m = a.length, n = a[0].length;
    int[] out = new int[m * n];
    int k = 0;
    for (int r = 0; r < m; r++) {
        if (r % 2 == 0) {
            for (int c = 0; c < n; c++) out[k++] = a[r][c];
        } else {
            for (int c = n - 1; c >= 0; c--) out[k++] = a[r][c];
        }
    }
    return out;
}

// Anti-diagonals: cells with the same r + c. There are m + n - 1 of them.
// Even d goes up and to the right. Odd d goes down and to the left. This is LeetCode 498.
static int[] diagonalZigzag(int[][] a) {
    int m = a.length, n = a[0].length;
    int[] out = new int[m * n];
    int k = 0;
    for (int d = 0; d <= m + n - 2; d++) {
        int rLow = Math.max(0, d - (n - 1));   // the smallest row on this anti-diagonal
        int rHigh = Math.min(d, m - 1);        // the largest row on this anti-diagonal
        if (d % 2 == 0) {
            for (int r = rHigh; r >= rLow; r--) out[k++] = a[r][d - r];
        } else {
            for (int r = rLow; r <= rHigh; r++) out[k++] = a[r][d - r];
        }
    }
    return out;
}

// Main diagonals: cells with the same r - c. Group them by the key r - c + (n - 1).
// This is the building block of LeetCode 1329 (sort each diagonal).
static void sortEachDiagonal(int[][] a) {
    int m = a.length, n = a[0].length;
    List<List<Integer>> groups = new ArrayList<>();
    for (int i = 0; i < m + n - 1; i++) groups.add(new ArrayList<>());
    for (int r = 0; r < m; r++) {
        for (int c = 0; c < n; c++) groups.get(r - c + n - 1).add(a[r][c]);
    }
    for (List<Integer> g : groups) Collections.sort(g);
    int[] next = new int[m + n - 1];            // next unread index in each sorted group
    for (int r = 0; r < m; r++) {
        for (int c = 0; c < n; c++) {
            int key = r - c + n - 1;
            a[r][c] = groups.get(key).get(next[key]++);
        }
    }
}
```

For an anti-diagonal with index `d`, the row range is `max(0, d - (n - 1))` to
`min(d, m - 1)`. The column is `d - r`. If you derive these two bounds once, no case analysis
for "hit the top wall" or "hit the right wall" remains.

### Template 3: Spiral with four shrinking boundaries

```java
static List<Integer> spiralOrder(int[][] a) {
    List<Integer> out = new ArrayList<>();
    int top = 0, bottom = a.length - 1, left = 0, right = a[0].length - 1;
    while (top <= bottom && left <= right) {
        for (int c = left; c <= right; c++) out.add(a[top][c]);       // left to right
        top++;
        for (int r = top; r <= bottom; r++) out.add(a[r][right]);     // top to bottom
        right--;
        if (top <= bottom) {                                          // a row remains
            for (int c = right; c >= left; c--) out.add(a[bottom][c]);
            bottom--;
        }
        if (left <= right) {                                          // a column remains
            for (int r = bottom; r >= top; r--) out.add(a[r][left]);
            left++;
        }
    }
    return out;
}
```

The two `if` guards stop a single remaining row or column from printing twice. Remove them and
the input `[[1, 2, 3]]` returns `[1, 2, 3, 2, 1]`.
Time is O(m * n), because the code visits each cell once. Extra space is O(1) besides the output.

### Template 4: Spiral with a direction array (generate a matrix)

```java
// LeetCode 59: fill an n x n matrix with 1..n*n in spiral order.
static int[][] generateSpiral(int n) {
    int[] dr = {-1, 0, 1, 0};                 // the same order as Template 1
    int[] dc = {0, 1, 0, -1};
    int[][] a = new int[n][n];
    int r = 0, c = 0, d = 1;                  // start moving right (index 1)
    for (int value = 1; value <= n * n; value++) {
        a[r][c] = value;
        int nr = r + dr[d], nc = c + dc[d];
        boolean outside = nr < 0 || nr >= n || nc < 0 || nc >= n;
        if (outside || a[nr][nc] != 0) {                   // wall or visited: turn right
            d = (d + 1) % 4;
            nr = r + dr[d];
            nc = c + dc[d];
        }
        r = nr;
        c = nc;
    }
    return a;
}
```

This form is shorter than the boundary form and works for any "walk and turn at an obstacle"
problem. A visited marker replaces the four boundary variables. The marker here is the value 0,
which is never a valid output.

### Template 5: Rotate 90 degrees clockwise in place

```java
// Transpose plus reverse each row.
static void rotateByTranspose(int[][] a) {
    int n = a.length;
    for (int r = 0; r < n; r++) {
        for (int c = r + 1; c < n; c++) {          // only above the diagonal
            int t = a[r][c]; a[r][c] = a[c][r]; a[c][r] = t;
        }
    }
    for (int[] row : a) {
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            int t = row[i]; row[i] = row[j]; row[j] = t;
        }
    }
}

// Layer swap: move four cells in one cycle. Each cell is read before it is overwritten.
static void rotateByLayers(int[][] a) {
    int n = a.length;
    for (int i = 0; i < n / 2; i++) {
        for (int j = i; j < n - 1 - i; j++) {
            int t = a[i][j];
            a[i][j] = a[n - 1 - j][i];                   // left column   -> top row
            a[n - 1 - j][i] = a[n - 1 - i][n - 1 - j];   // bottom row    -> left column
            a[n - 1 - i][n - 1 - j] = a[j][n - 1 - i];   // right column  -> bottom row
            a[j][n - 1 - i] = t;                         // top row       -> right column
        }
    }
}

// Rotation of a rectangular m x n matrix cannot be in place. Return a new n x m matrix.
static int[][] rotateRectangle(int[][] a) {
    int m = a.length, n = a[0].length;
    int[][] out = new int[n][m];
    for (int r = 0; r < m; r++) {
        for (int c = 0; c < n; c++) out[c][m - 1 - r] = a[r][c];
    }
    return out;
}
```

The transpose loop starts the inner index at `r + 1`. If it starts at 0, every pair swaps twice
and the matrix does not change. For the layer swap, layer `i` has `n - 2 * i` cells per side,
and the inner loop covers `n - 2 * i - 1` of them. The last cell of each side belongs to the
next side's cycle. For odd `n`, the center cell stays in place, and `n / 2` rounds down so the
outer loop skips it.

| Rotation | Target of cell `(r, c)` in an `n x n` matrix | By transforms |
|---|---|---|
| 90 degrees clockwise | `(c, n - 1 - r)` | transpose, then reverse each row |
| 90 degrees counterclockwise | `(n - 1 - c, r)` | transpose, then reverse each column |
| 180 degrees | `(n - 1 - r, n - 1 - c)` | reverse each row, then reverse each column |
| Flip left-right | `(r, n - 1 - c)` | reverse each row |
| Flip top-bottom | `(n - 1 - r, c)` | reverse each column |

### Template 6: In-place marking with O(1) extra space (Set Matrix Zeroes)

```java
static void setZeroes(int[][] a) {
    int m = a.length, n = a[0].length;
    boolean firstRowZero = false, firstColZero = false;
    for (int c = 0; c < n; c++) if (a[0][c] == 0) firstRowZero = true;
    for (int r = 0; r < m; r++) if (a[r][0] == 0) firstColZero = true;

    // Pass 1: record each zero in the first row and the first column.
    for (int r = 1; r < m; r++) {
        for (int c = 1; c < n; c++) {
            if (a[r][c] == 0) { a[r][0] = 0; a[0][c] = 0; }
        }
    }
    // Pass 2: zero every inner cell whose row marker or column marker is 0.
    for (int r = 1; r < m; r++) {
        for (int c = 1; c < n; c++) {
            if (a[r][0] == 0 || a[0][c] == 0) a[r][c] = 0;
        }
    }
    // Pass 3: handle the first row and the first column last, from the saved booleans.
    if (firstRowZero) for (int c = 0; c < n; c++) a[0][c] = 0;
    if (firstColZero) for (int r = 0; r < m; r++) a[r][0] = 0;
}
```

The order of the passes matters. Pass 2 must read the markers before pass 3 overwrites them.
Time is O(m * n). Space is O(1).

### Template 7: State encoding for simultaneous updates (Game of Life)

```java
// LeetCode 289. Bit 0 = current state. Bit 1 = next state.
static void gameOfLife(int[][] board) {
    int m = board.length, n = board[0].length;
    for (int r = 0; r < m; r++) {
        for (int c = 0; c < n; c++) {
            int live = 0;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr, nc = c + dc;
                    if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;       // outside the board
                    live += board[nr][nc] & 1;                              // current state only
                }
            }
            boolean aliveNow = (board[r][c] & 1) == 1;
            boolean aliveNext = aliveNow ? (live == 2 || live == 3) : live == 3;
            if (aliveNext) board[r][c] |= 2;                                // set bit 1
        }
    }
    for (int r = 0; r < m; r++) {
        for (int c = 0; c < n; c++) board[r][c] >>= 1;                      // next becomes current
    }
}
```

Every cell must see the old state of its neighbors. If the code wrote the new value directly, a
later cell would read a neighbor that already changed. The two-bit encoding removes that problem
without a copy. Time is O(m * n) and space is O(1).

The follow-up "the board is infinite" has a different answer. A live cell can reach outside
the array, so you store only the live cells in a `Set` and count neighbors with a
`Map<Long, Integer>` of candidate cells. One step costs O(L), where L is the number of live
cells. Template 14 shows the code.

### Template 8: Search in a sorted matrix

```java
// LeetCode 240: rows sorted, columns sorted. Staircase from the top-right corner.
static boolean searchStaircase(int[][] a, int target) {
    int r = 0, c = a[0].length - 1;
    while (r < a.length && c >= 0) {
        if (a[r][c] == target) return true;
        if (a[r][c] > target) c--;        // everything below a[r][c] in this column is larger
        else r++;                         // everything left of a[r][c] in this row is smaller
    }
    return false;
}

// LeetCode 74: each row is sorted and the first value of a row is greater than the last value
// of the previous row. The matrix is one sorted array in row-major order.
static boolean searchFlattened(int[][] a, int target) {
    int m = a.length, n = a[0].length;
    int lo = 0, hi = m * n - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        int v = a[mid / n][mid % n];       // index -> (row, column)
        if (v == target) return true;
        if (v < target) lo = mid + 1;
        else hi = mid - 1;
    }
    return false;
}
```

The staircase search takes at most `m + n` steps, because each step removes one row or one column.
The flattened search takes O(log(m * n)) steps. The staircase also works for LeetCode 74, but it
is slower than the binary search. The binary search does not work for LeetCode 240, because the
last value of a row can be larger than the first value of the next row.

Start at the top-right or at the bottom-left corner. Do not start at the top-left or
the bottom-right corner. At those corners both moves go to larger values (or both go to smaller
values), so a comparison cannot tell you which direction to take.

### Template 9: Simulation with direction state (walking robot)

```java
// LeetCode 874. Commands: -2 = turn left, -1 = turn right, k > 0 = move k steps one by one.
// Return the largest squared distance from the origin at any time.
static int robotSim(int[] commands, int[][] obstacles) {
    int[] dx = {0, 1, 0, -1};                          // north, east, south, west
    int[] dy = {1, 0, -1, 0};                          // north is +y on this plane
    Set<Long> blocked = new HashSet<>();
    for (int[] o : obstacles) blocked.add(key(o[0], o[1]));
    int x = 0, y = 0, d = 0, best = 0;                 // d = 0 means facing north
    for (int cmd : commands) {
        if (cmd == -2) d = (d + 3) % 4;
        else if (cmd == -1) d = (d + 1) % 4;
        else {
            for (int step = 0; step < cmd; step++) {
                int nx = x + dx[d], ny = y + dy[d];
                if (blocked.contains(key(nx, ny))) break;
                x = nx;
                y = ny;
                best = Math.max(best, x * x + y * y);   // LC 874: the answer is < 2^31
            }
        }
    }
    return best;
}

// Pack two ints into one long. A Set<int[]> does not work: arrays compare by identity.
static long key(int x, int y) {
    return ((long) x << 32) | (y & 0xFFFFFFFFL);
}
```

Move one step at a time, so that an obstacle stops the robot at the exact cell. Moving `cmd`
steps in one jump skips the obstacle check.

LC 874 uses `(x, y)` points on a plane, with north as +y. Grid problems use `(row, column)`, with
up as row - 1. The `dx` and `dy` arrays keep the same clockwise order as `DR` and `DC` in
Template 1, so the turn formulas `(d + 1) % 4` and `(d + 3) % 4` do not change.

### Template 10: Cycle detection in simulated states

```java
// LeetCode 957: 8 cells. A cell becomes 1 if both neighbors are equal, else 0. Ends become 0.
// Encode the 8 cells as one int. Remember the day on which each state first appeared.
static int[] prisonAfterNDays(int[] cells, int n) {
    int state = 0;
    for (int cell : cells) state = (state << 1) | cell;

    Map<Integer, Integer> firstDay = new HashMap<>();
    boolean skipped = false;
    for (int day = 0; day < n; day++) {
        if (!skipped) {
            Integer earlier = firstDay.put(state, day);
            if (earlier != null) {                      // this state has appeared before
                int period = day - earlier;
                n = day + (n - day) % period;           // jump over whole periods
                skipped = true;
                if (day >= n) break;
            }
        }
        state = ~((state << 1) ^ (state >> 1)) & 0x7E;  // bit i = 1 if its two neighbors match
    }
    int[] out = new int[8];
    for (int i = 0; i < 8; i++) out[i] = (state >> (7 - i)) & 1;
    return out;
}

// LeetCode 1041: does the robot stay in a bounded circle if it repeats the instructions forever?
// After one pass, check the position and the direction. No state set is needed.
static boolean isRobotBounded(String instructions) {
    int[] dx = {0, 1, 0, -1};                          // north, east, south, west
    int[] dy = {1, 0, -1, 0};
    int x = 0, y = 0, d = 0;
    for (char ch : instructions.toCharArray()) {
        if (ch == 'L') d = (d + 3) % 4;
        else if (ch == 'R') d = (d + 1) % 4;
        else { x += dx[d]; y += dy[d]; }
    }
    return (x == 0 && y == 0) || d != 0;
}
```

The state map holds at most `min(n, number of states)` entries. The input state on day 0 can be
any of the 2^8 = 256 states. From day 1 on, the two end cells are always 0, so at most 2^6 = 64
states remain. Among the 65 states of days 1 to 65, two must be equal (the pigeonhole
principle). So the loop finds a repeat at iteration `day = 65` at the latest. An exhaustive run
over all 256 inputs gives a tighter fact: the first repeat comes at day 15 or earlier, and the
period is 1, 7, or 14. Period 1 is real: `00100100` maps to itself. After the jump, fewer than
14 steps remain. Each step is O(1), so the whole method is O(1) for any `n`.

The second method uses a mathematical argument in place of a state set. If the robot
faces a new direction after one pass, four passes turn it a full circle, and the displacement
sums to zero. If it faces north and is not at the origin, the displacement repeats and the
robot drifts away.

### Template 11: Zigzag string placement

```java
// LeetCode 6: write the string in a zigzag over numRows rows, then read row by row.
static String convertZigzag(String s, int numRows) {
    if (numRows == 1 || numRows >= s.length()) return s;
    StringBuilder[] rows = new StringBuilder[numRows];
    for (int i = 0; i < numRows; i++) rows[i] = new StringBuilder();
    int row = 0, step = 1;
    for (char ch : s.toCharArray()) {
        rows[row].append(ch);
        if (row == 0) step = 1;                  // bounce off the top
        else if (row == numRows - 1) step = -1;  // bounce off the bottom
        row += step;
    }
    StringBuilder out = new StringBuilder();
    for (StringBuilder sb : rows) out.append(sb);
    return out.toString();
}
```

The `numRows == 1` guard is required. With one row, `row == 0` and `row == numRows - 1` are both
true, and the walk would leave the array.

### Template 12: Walk an unbounded spiral and keep the cells inside (Spiral Matrix III)

```java
// LeetCode 885: start at (rStart, cStart) facing east. Walk a clockwise spiral that may leave
// the grid. Return the grid cells in the order of the first visit.
static int[][] spiralMatrixIII(int rows, int cols, int rStart, int cStart) {
    int[] dr = {-1, 0, 1, 0};                 // up, right, down, left (Template 1 order)
    int[] dc = {0, 1, 0, -1};
    int[][] out = new int[rows * cols][];
    int r = rStart, c = cStart, k = 0, d = 1;  // d = 1: facing east
    out[k++] = new int[] {r, c};
    for (int len = 1; k < rows * cols; len++) {
        for (int side = 0; side < 2; side++) {     // each length serves two sides: 1, 1, 2, 2, ...
            for (int step = 0; step < len; step++) {
                r += dr[d];
                c += dc[d];
                if (r >= 0 && r < rows && c >= 0 && c < cols) out[k++] = new int[] {r, c};
            }
            d = (d + 1) % 4;
        }
    }
    return out;
}
```

The side lengths are 1, 1, 2, 2, 3, 3, and so on: east 1, south 1, west 2, north 2, east 3. The
walk visits each cell of the plane at most once, so `k` never passes `rows * cols`. Every grid
cell lies at most `max(rows, cols) - 1` rows and columns away from the start. The spiral covers
that square around the start within `(2 * max(rows, cols) + 1)^2` steps, so the time is
O(max(rows, cols)^2).

### Template 13: Ring rotation and gravity (Cyclically Rotating a Grid, Rotating the Box)

```java
// LeetCode 1914: rotate every layer counterclockwise k times. m and n are even.
static int[][] rotateGrid(int[][] grid, int k) {
    int m = grid.length, n = grid[0].length;
    for (int layer = 0; layer < Math.min(m, n) / 2; layer++) {
        int top = layer, left = layer, bottom = m - 1 - layer, right = n - 1 - layer;
        List<int[]> ring = new ArrayList<>();             // the ring cells in clockwise order
        for (int c = left; c < right; c++) ring.add(new int[] {top, c});
        for (int r = top; r < bottom; r++) ring.add(new int[] {r, right});
        for (int c = right; c > left; c--) ring.add(new int[] {bottom, c});
        for (int r = bottom; r > top; r--) ring.add(new int[] {r, left});
        int len = ring.size();                            // 2 * (height + width) - 4
        int[] values = new int[len];
        for (int i = 0; i < len; i++) values[i] = grid[ring.get(i)[0]][ring.get(i)[1]];
        int shift = k % len;                              // whole turns change nothing
        for (int i = 0; i < len; i++) {
            int[] cell = ring.get(i);
            grid[cell[0]][cell[1]] = values[(i + shift) % len];   // take from i + k
        }
    }
    return grid;
}

// LeetCode 1861: '#' = stone, '*' = obstacle, '.' = empty. Rotate the box 90 degrees clockwise,
// then let the stones fall. Falling down after the rotation is sliding right before it.
// This method changes the input rows.
static char[][] rotateTheBox(char[][] box) {
    int m = box.length, n = box[0].length;
    char[][] out = new char[n][m];
    for (int r = 0; r < m; r++) {
        int free = n - 1;                                 // the cell where the next stone lands
        for (int c = n - 1; c >= 0; c--) {
            if (box[r][c] == '*') {
                free = c - 1;                             // a stone cannot pass an obstacle
            } else if (box[r][c] == '#') {
                box[r][c] = '.';
                box[r][free] = '#';
                free--;
            }
        }
        for (int c = 0; c < n; c++) out[c][m - 1 - r] = box[r][c];   // Template 5 formula
    }
    return out;
}
```

A layer with height `h` and width `w` has `2 * (h + w) - 4` cells, because the four corners
belong to two sides each. `k` can be up to 10^9 in LC 1914, so reduce it with `k % len` before
you move anything. In the clockwise list, a counterclockwise step moves each value one position
back, so position `i` receives the value from position `i + k`. Both methods run in O(m * n).

### Template 14: Game of Life on an unbounded board (sparse set of live cells)

```java
// One generation. live holds the packed (r, c) keys of the live cells.
static Set<Long> lifeStep(Set<Long> live) {
    Map<Long, Integer> neighbors = new HashMap<>();       // candidate cell -> live neighbor count
    for (long cell : live) {
        int r = (int) (cell >> 32), c = (int) cell;       // unpack the two ints
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                neighbors.merge(pack(r + dr, c + dc), 1, Integer::sum);
            }
        }
    }
    Set<Long> next = new HashSet<>();
    for (Map.Entry<Long, Integer> e : neighbors.entrySet()) {
        int count = e.getValue();
        if (count == 3 || (count == 2 && live.contains(e.getKey()))) next.add(e.getKey());
    }
    return next;                                          // a cell with no live neighbor dies
}

static long pack(int r, int c) {
    return ((long) r << 32) | (c & 0xFFFFFFFFL);
}
```

Only a cell next to a live cell can be alive in the next generation, so the map holds at most
`8 * L` candidates, where `L` is the number of live cells. One generation costs O(L) hash
operations, and the board size does not appear in the cost.

---

## 7. Variations of the Pattern

### Variation A: Traversal orders
**Problem Type:** Spiral (LC 54, 59, 885, 2326), diagonal zigzag (LC 498), Matrix Diagonal Sum
(LC 1572), Sort the Matrix Diagonally (LC 1329), column order, snake order, zigzag string (LC 6).
**Key Insight:** Express the order as bounds that shrink (spiral), as a constant `r + c` or
`r - c` (diagonals), or as a bouncing index (zigzag).
**Technique:** Templates 2, 3, 4, 11, and 12. For LC 1572, add `a[i][i]` and `a[i][n - 1 - i]`
for each `i`, and subtract the center cell once if `n` is odd.

### Variation B: Rotation and reflection
**Problem Type:** Rotate Image (LC 48), Transpose Matrix (LC 867), Flipping an Image (LC 832),
Rotating the Box (LC 1861), Cyclically Rotating a Grid (LC 1914).
**Key Insight:** A rotation is a transpose plus a reflection. Memorize the table in Template 5.
**Technique:** Template 5 and Template 13. LC 1914 rotates each layer counterclockwise. Treat
each layer as a ring: read the ring into a list, rotate the list by `k % ringLength`, and write it
back. A layer of height `h` and width `w` has `ringLength = 2 * (h + w) - 4`. LC 832 reverses
each row and then inverts each bit, in one pass per row.

### Variation C: In-place marking
**Problem Type:** Set Matrix Zeroes (LC 73), Game of Life (LC 289).
**Key Insight:** Hold two pieces of information in one cell, or use the matrix border as scratch.
**Technique:** Templates 6 and 7. Other encodings also work: a value outside the input range
(for example `2` for "was 1, now 0" and `-1` for "was 0, now 1"), or a negative sign as a mark.

### Variation D: Sorted matrix search
**Problem Type:** Search a 2D Matrix (LC 74), Search a 2D Matrix II (LC 240), Kth Smallest
Element in a Sorted Matrix (LC 378).
**Key Insight:** If only rows are sorted, use binary search per row. If each row starts above the
previous end, flatten the index. If rows and columns are sorted, use the staircase.
**Technique:** Template 8. LC 378 is a binary search on the answer value, covered in
[Binary Search on Answer](../06-binary-search-on-answer/README.md), or a heap
([Heap](../16-heap-priority-queue/README.md)).

### Variation E: Simulation with a direction and a set of obstacles
**Problem Type:** Walking Robot Simulation (LC 874), Robot Return to Origin (LC 657), Spiral
Matrix III (LC 885), Count Unguarded Cells in the Grid (LC 2257).
**Key Insight:** Keep `(x, y, d)` as the state. Apply one command at a time. Store obstacles in
a hash set, because a lookup in a `List` is O(k).
**Technique:** Template 9. For LC 885 the spiral leaves the matrix and returns. Walk the infinite
spiral with step lengths `1, 1, 2, 2, 3, 3, ...` and record only the cells that are inside
(Template 12). For LC 2257, walk from each guard in the four directions until a wall or another
guard stops the walk, and mark each cell on the way as guarded.

### Variation F: Cycle detection in states
**Problem Type:** Prison Cells After N Days (LC 957), Robot Bounded In Circle (LC 1041),
Walking Robot Simulation II (LC 2069).
**Key Insight:** If the state space is small and the next state depends only on the current
state, the sequence is eventually periodic. Skip whole periods with `%`.
**Technique:** Template 10. For a state that is too large for a hash set, use Floyd's cycle
detection from [Fast & Slow Pointers](../03-fast-slow-pointers/README.md). It needs O(1) memory
and only an equality test on two states. LC 2069 needs no state set at all: the robot walks only
on the border, so its position repeats every `2 * (width + height) - 4` steps, and you reduce
each move with `%`. One trap: after the robot returns to `(0, 0)` by moving, it faces South, not
East. Only the robot that has not moved yet faces East.

### Variation G: Region indexing (box of a Sudoku)
**Problem Type:** Valid Sudoku (LC 36). Check if Every Row and Column Contains All Numbers
(LC 2133) uses only the row and column checks. Sudoku Solver (LC 37, Hard) adds
[Backtracking](../08-recursion-backtracking/README.md) to the same region index.
**Key Insight:** Map a cell to its 3 x 3 box with `(r / 3) * 3 + c / 3`. One pass with three
sets (or three boolean tables) per row, column, and box checks every rule.
**Technique:** Use `boolean[9][9]` tables for rows, columns, and boxes. For a cell `(r, c)` that
holds a digit, set `v = board[r][c] - '1'`, so `v` is in the range 0 to 8. The cell conflicts if
`rows[r][v]`, `cols[c][v]`, or `boxes[(r / 3) * 3 + c / 3][v]` is already set. (Indexing a
`[9][9]` table by the digit itself throws an exception for the digit 9.)

### Variation H: Reshape and re-index
**Problem Type:** Reshape the Matrix (LC 566), Convert 1D Array Into 2D Array (LC 2022), Shift
2D Grid (LC 1260).
**Key Insight:** Flatten with `k = r * n + c`. Unflatten with `r = k / n` and `c = k % n`. A
reshape is an unflatten with a different `n`. For LC 1260, the value at flat index `k` moves to
flat index `(k + shift) % (m * n)`.

### Variation I: Simulation with a cascading rule
**Problem Type:** Candy Crush (LC 723), Rotating the Box (LC 1861).
**Key Insight:** Split one step into phases that each have a clear rule: mark, remove, drop.
Repeat until no change. Do not mix the phases in one pass. LC 723 is a premium problem.
Template 13 shows the drop phase for LC 1861.

---

## 8. Classic Interview Problems

Difficulties come from the LeetCode problem list at `https://leetcode.com/api/problems/all/`.
The list has 12 problems in order of difficulty. Section 7 names more problems for each
variation.

### EASY (Warm-up)
| Problem | Key Insight | LC # | Difficulty |
|---------|-------------|------|------------|
| **Transpose Matrix** | `out[c][r] = a[r][c]`; the output is `n x m` for rectangles | 867 | Easy |

### MEDIUM (Interview Favorites)
| Problem | Key Insight | LC # | Difficulty |
|---------|-------------|------|------------|
| **Spiral Matrix** | Four shrinking boundaries with two guards for a single row or column | 54 | Medium |
| **Rotate Image** | Transpose, then reverse each row; or swap four cells per cycle | 48 | Medium |
| **Set Matrix Zeroes** | Use the first row and column as markers; save their own state in two booleans | 73 | Medium |
| **Game of Life** | Two-bit state encoding; read `& 1`, write bit 1, then shift | 289 | Medium |
| **Search a 2D Matrix** | Flattened binary search with `mid / n` and `mid % n` | 74 | Medium |
| **Search a 2D Matrix II** | Staircase from the top-right in O(m + n) | 240 | Medium |
| **Diagonal Traverse** | Group by `r + c`; reverse every other anti-diagonal | 498 | Medium |
| **Zigzag Conversion** | Row index that bounces between 0 and `numRows - 1` | 6 | Medium |
| **Walking Robot Simulation** | Direction index, obstacle hash set, one step at a time | 874 | Medium |
| **Robot Bounded In Circle** | One pass: back at origin or not facing north means bounded | 1041 | Medium |
| **Prison Cells After N Days** | Encode the state; find the cycle; skip periods | 957 | Medium |

### HARD (Differentiators)

No problem in the list is Hard. Every problem that section 7 names is Easy or Medium, except
Sudoku Solver (LC 37, Hard), which is a backtracking problem. These problems cost candidates
points on precision, not on the idea. Interviewers raise the difficulty with follow-ups on the
same problems, for example Game of Life on an infinite board (Template 14) and rotation with
O(1) extra space (Template 5).

---

## 9. How to Practice This Pattern Effectively

### Week 1: Index discipline
**Day 1:**
- Write `DR`, `DC`, and `inBounds` from memory.
- Solve "Transpose Matrix" and "Reshape the Matrix".

**Day 2:**
- Solve "Spiral Matrix" with the four-boundary form.
- Test it on `[[1,2,3]]`, `[[1],[2],[3]]`, and a 1 x 1 matrix.

**Day 3:**
- Solve "Spiral Matrix II" with a direction array.
- Solve "Spiral Matrix" again with the direction array and a visited marker.

**Day 4:**
- Solve "Rotate Image" by transpose and by layers.
- Draw the four-cell cycle on paper before you code it.

**Day 5:**
- Solve "Diagonal Traverse" and "Zigzag Conversion".
- Derive the row bounds of an anti-diagonal yourself.

### Week 2: In-place work and simulation
**Day 1:**
- Solve "Set Matrix Zeroes" with O(m + n) space, then with O(1) space.

**Day 2:**
- Solve "Game of Life". Write the two-bit encoding first, then the loops.
- Answer the infinite-board follow-up out loud.

**Day 3:**
- Solve "Search a 2D Matrix" and "Search a 2D Matrix II".
- Explain why the top-left corner does not work for the staircase.

**Day 4:**
- Solve "Walking Robot Simulation" and "Robot Bounded In Circle".

**Day 5:**
- Solve "Prison Cells After N Days".
- Solve "Valid Sudoku" and "Rotating the Box" (Template 13).

### Self-Check Questions:
- Can I write all four moves and the turn formulas without looking?
- Can I name the new position of a cell `(r, c)` for each rotation?
- Can I say why Set Matrix Zeroes needs two extra booleans?
- Do I know which corner I start from in the staircase search, and why?
- Can I explain when a simulation has a cycle, and what proves it?
- Do I test the 1 x n, m x 1, and 1 x 1 cases before I declare the code done?

---

## 10. Common Mistakes Even Experienced Developers Make

### Mistake 1: Swapping rows and columns (`m` and `n`)
<!-- fragment -->
```java
// WRONG: the loop bound uses the wrong dimension on a non-square matrix.
for (int r = 0; r < a[0].length; r++) { /* ... */ }

// CORRECT: a.length is the number of rows. a[0].length is the number of columns.
for (int r = 0; r < a.length; r++) { /* ... */ }
```
Name the variables `rows` and `cols`. A one-letter `n` for both gives wrong answers on rectangles.

### Mistake 2: A negative result from `%` in a left turn
<!-- fragment -->
```java
// WRONG: if d = 0, (d - 1) % 4 is -1 in Java, and DR[-1] throws an exception.
d = (d - 1) % 4;

// CORRECT: add 3 (a left turn is three right turns).
d = (d + 3) % 4;
```

### Mistake 3: Reading a cell after it has changed
<!-- fragment -->
```java
// WRONG: the cell gets its next state at once. The cells after it in the same pass count
// this cell with its next state, not with its current state.
live += a[nr][nc];                         // in the neighbor loop
a[r][c] = aliveNext ? 1 : 0;               // after the neighbor loop

// CORRECT: store the next state in bit 1 and read only bit 0. Shift every cell after the pass.
live += a[nr][nc] & 1;                     // in the neighbor loop
if (aliveNext) a[r][c] |= 2;               // after the neighbor loop
```
Template 7 shows the full method.

### Mistake 4: Transposing the full square
<!-- fragment -->
```java
// WRONG: every pair swaps twice, so the matrix returns to the original.
for (int c = 0; c < n; c++) { /* swap a[r][c], a[c][r] */ }

// CORRECT: swap only the cells above the diagonal.
for (int c = r + 1; c < n; c++) { /* swap a[r][c], a[c][r] */ }
```

### Mistake 5: No guards for a single row or column in a spiral
Without the `if (top <= bottom)` and `if (left <= right)` checks, the last row or column prints
again in reverse. Test `[[1,2,3]]` and `[[1],[2],[3]]` every time.

### Mistake 6: Zeroing the first row or column too early
<!-- fragment -->
```java
// WRONG: the first row is zeroed before pass 2. Every column marker a[0][c] is now 0,
// so pass 2 zeroes every inner cell, even in columns that have no zero.
if (firstRowZero) for (int c = 0; c < n; c++) a[0][c] = 0;
for (int r = 1; r < m; r++) {
    for (int c = 1; c < n; c++) {
        if (a[r][0] == 0 || a[0][c] == 0) a[r][c] = 0;
    }
}

// CORRECT: pass 2 reads the markers first. The first row and column change last.
for (int r = 1; r < m; r++) {
    for (int c = 1; c < n; c++) {
        if (a[r][0] == 0 || a[0][c] == 0) a[r][c] = 0;
    }
}
if (firstRowZero) for (int c = 0; c < n; c++) a[0][c] = 0;
if (firstColZero) for (int r = 0; r < m; r++) a[r][0] = 0;
```

### Mistake 7: Shared row objects
<!-- fragment -->
```java
// WRONG: every row is the same array. Writing a[0][0] changes every row.
int[] row = new int[n];
int[][] a = new int[m][];
Arrays.fill(a, row);

// CORRECT: allocate one array per row.
int[][] b = new int[m][n];
```
Also remember that `a.clone()` copies only the outer array. The rows are shared. Copy each row
with `a[r].clone()` or `Arrays.copyOf(a[r], n)`.

### Mistake 8: Using `int[]` as a set element or map key
`Set<int[]>` compares arrays by identity. A lookup of `new int[]{1, 2}` never finds a stored
`{1, 2}`. Encode the cell as a `long`, as an `int` (`r * cols + c`), or as a `String`. The
[Java Toolkit](../java-toolkit.md) lists the collection APIs that these keys use.

### Mistake 9: Moving a robot in one jump
Skipping the per-step obstacle check lets the robot jump over an obstacle. Move one unit at a
time, or compute the first obstacle on the line.

### Mistake 10: Marking with a value that is valid input
If you mark "visited" with `0` and `0` is a real cell value, the code confuses the two. Pick a
marker outside the input range, or use a separate `boolean[][]`.

### Mistake 11: Not checking for a cycle when the step count is huge
A loop to 10^9 steps times out. If the state is small, store each state with the step number
at which it first appeared. When a state repeats, apply `%` on the period.

### Mistake 12: Off-by-one at the end of a bounds range
Write the bound of every loop as an inclusive or exclusive range and keep one style.
Common errors: `c <= n` where `c < n` is meant, and `mid / m` (the number of rows) where the
flattened index needs `mid / n` (the number of columns). On a square matrix both give the same
result, so only a rectangular test finds the second error. Run the smallest inputs (1 x 1,
1 x n, m x 1, 2 x 2, 2 x 3) by hand.

---

## Commonly Confused With

| Looks like... | But use... | The question that decides |
|---|---|---|
| "Number of islands" or "shortest path in a binary matrix" | [Graph Traversal (Pattern 11)](../11-graph-traversal/README.md) | Does the answer depend on connectivity between cells or on a shortest distance? If yes, treat the grid as a graph and search it |
| "Unique paths", "minimum path sum", "maximal square" | [Dynamic Programming (Pattern 15)](../15-dynamic-programming/README.md) | Does the answer for a cell come from the answers of its neighbors above and to the left? If yes, fill a DP table |
| "Sum of a rectangle" with many queries | [Prefix Sum (Pattern 04)](../04-prefix-sum/README.md) | Does the problem ask for sums of sub-rectangles? If yes, build a 2D prefix sum |
| "Kth smallest in a sorted matrix" | [Binary Search on Answer (Pattern 06)](../06-binary-search-on-answer/README.md) | Is the answer a value that you can count against (how many cells are `<= x`)? If yes, binary search on the value |
| "Search in a sorted matrix" (staircase) | Binary search on a flattened index (LC 74) | Does each row start above the end of the previous row? If yes, flatten and binary search; if only rows and columns are sorted, use the staircase |
| "Simulate n steps" with a huge `n` | Cycle detection by state (Template 10) | Is the state space small and the rule deterministic? If yes, find the period and skip |
| "Place queens", "solve Sudoku" | [Backtracking (Pattern 08)](../08-recursion-backtracking/README.md) | Do you choose, check, and undo? If yes, it is a search, not a simulation |

> Full decision procedure: [How to Choose a Pattern](../how-to-choose-a-pattern.md)

---

## Quick Reference Card

```text
┌─────────────────────────────────────────────────────────────────┐
│               MATRIX & SIMULATION CHEAT SHEET                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  BASICS:                                                         │
│  - rows = a.length, cols = a[0].length                           │
│  - DR = {-1,0,1,0}, DC = {0,1,0,-1}  (up, right, down, left)     │
│  - turn right: (d+1)%4     turn left: (d+3)%4                    │
│  - flatten k = r*cols + c   unflatten r = k/cols, c = k%cols     │
│  - diagonal: r-c constant   anti-diagonal: r+c constant          │
│  - Sudoku box: (r/3)*3 + c/3                                     │
│                                                                  │
│  ROTATE n x n (clockwise): (r,c) -> (c, n-1-r)                   │
│  - transpose (c from r+1), then reverse each row                 │
│  - counterclockwise: transpose, then reverse each column         │
│                                                                  │
│  SPIRAL: top, bottom, left, right; guard the last two sides      │
│                                                                  │
│  IN PLACE:                                                       │
│  - Set zeroes: first row/col = markers + 2 booleans; fix last    │
│  - Game of Life: bit0 = now, bit1 = next; read &1; final >>= 1   │
│                                                                  │
│  SORTED MATRIX:                                                  │
│  - rows and cols sorted: staircase from top-right, O(m + n)      │
│  - flattened sorted: binary search, mid -> (mid/n, mid%n)        │
│                                                                  │
│  SIMULATION:                                                     │
│  - one step per loop; obstacles in a HashSet of long keys        │
│  - huge step count: store state -> first step; skip with %       │
│                                                                  │
│  TEST FIRST: 1x1, 1xn, mx1, 2x2, all equal, all zero             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```
