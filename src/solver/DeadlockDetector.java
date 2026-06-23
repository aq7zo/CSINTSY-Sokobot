package solver;

import java.util.ArrayDeque;

/**
 * Deadlock detection (Blueprint section 5). Implements two of the four categories:
 *   - 5.1 static dead squares (reverse-BFS "pull" from every target), and
 *   - 5.2 dynamic freeze deadlocks (a crate, or a cluster of crates, that can never move again).
 *
 * 5.3 (corridor deadlocks) is deliberately deferred: most corridor traps already fall out as static
 * dead squares or freezes, and an unsound corridor check would risk pruning solvable states (a
 * false-positive deadlock = a missed solve). See CHECKLIST.md for this deviation.
 *
 * Every check here is SOUND: it only flags a state when the crate(s) provably can never reach a
 * goal. Missing a real deadlock (false negative) merely wastes time; wrongly flagging a live state
 * (false positive) would lose solutions, so we never do that.
 */
public class DeadlockDetector {
  private final Board b;
  /** Simple static dead squares: a crate pushed here can never reach any target. */
  public final boolean[] dead;
  /** Recursion guard for the freeze check: cells currently assumed frozen act as walls. */
  private final boolean[] inStack;

  public DeadlockDetector(Board b) {
    this.b = b;
    this.dead = computeDeadSquares();
    this.inStack = new boolean[b.numCells];
  }

  // ---- 5.1 static dead squares -------------------------------------------------------------

  /**
   * A cell is "live" if a crate placed there could be pushed to some target. Compute liveness by
   * reverse BFS from every target: if a crate can be at B (live), it could have been pushed there
   * from A = B - D provided A is floor and the player's standing cell A - D is floor. Everything
   * not marked live (and not a wall) is a dead square.
   */
  private boolean[] computeDeadSquares() {
    boolean[] live = new boolean[b.numCells];
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    for (int t : b.targetCells) {
      if (!live[t]) {
        live[t] = true;
        queue.add(t);
      }
    }
    while (!queue.isEmpty()) {
      int cur = queue.poll();
      int cr = b.rowOf(cur);
      int cc = b.colOf(cur);
      for (int d = 0; d < 4; d++) {
        int ar = cr - Board.DR[d];      // crate's previous cell
        int ac = cc - Board.DC[d];
        int pr = cr - 2 * Board.DR[d];  // player's standing cell
        int pc = cc - 2 * Board.DC[d];
        if (!b.inBounds(ar, ac) || !b.inBounds(pr, pc)) {
          continue;
        }
        int a = b.cellId(ar, ac);
        int p = b.cellId(pr, pc);
        if (b.wall[a] || b.wall[p] || live[a]) {
          continue;
        }
        live[a] = true;
        queue.add(a);
      }
    }
    boolean[] result = new boolean[b.numCells];
    for (int i = 0; i < b.numCells; i++) {
      result[i] = !b.wall[i] && !live[i];
    }
    return result;
  }

  // ---- 5.2 freeze (group) deadlocks --------------------------------------------------------

  /**
   * Returns true if, given crate occupancy {@code occ}, the box that was just pushed to
   * {@code pushedCell} creates a freeze deadlock. We check the pushed box itself and each of its
   * box-neighbors, because a push can also pin a previously-free neighbor against a wall.
   */
  public boolean isFreezeDeadlock(boolean[] occ, int pushedCell) {
    if (frozenAndOffGoal(occ, pushedCell)) {
      return true;
    }
    int r = b.rowOf(pushedCell);
    int c = b.colOf(pushedCell);
    for (int d = 0; d < 4; d++) {
      int nr = r + Board.DR[d];
      int nc = c + Board.DC[d];
      if (!b.inBounds(nr, nc)) {
        continue;
      }
      int n = b.cellId(nr, nc);
      if (occ[n] && frozenAndOffGoal(occ, n)) {
        return true;
      }
    }
    return false;
  }

  /** A frozen box that is not on a goal makes the whole position unsolvable. */
  private boolean frozenAndOffGoal(boolean[] occ, int cell) {
    return !b.target[cell] && isFrozen(occ, cell);
  }

  /** A box is frozen iff it cannot move along either axis (recursively, mutual support included). */
  private boolean isFrozen(boolean[] occ, int cell) {
    inStack[cell] = true;
    boolean blocked = blockedAlongAxis(occ, cell, 2, 3)   // horizontal: left, right
        && blockedAlongAxis(occ, cell, 0, 1);             // vertical: up, down
    inStack[cell] = false;
    return blocked;
  }

  /**
   * Is the box at {@code cell} unable to move along the axis spanned by directions dA/dB?
   * Blocked if: a wall (or an assumed-frozen box) sits on either side; OR both destination cells
   * are static dead squares; OR a neighboring box on that axis is itself frozen.
   */
  private boolean blockedAlongAxis(boolean[] occ, int cell, int dA, int dB) {
    int r = b.rowOf(cell);
    int c = b.colOf(cell);
    int ar = r + Board.DR[dA];
    int ac = c + Board.DC[dA];
    int br = r + Board.DR[dB];
    int bc = c + Board.DC[dB];

    boolean side1Wall = !b.inBounds(ar, ac) || b.wall[b.cellId(ar, ac)];
    boolean side2Wall = !b.inBounds(br, bc) || b.wall[b.cellId(br, bc)];
    if (side1Wall || side2Wall) {
      return true;
    }

    int n1 = b.cellId(ar, ac);
    int n2 = b.cellId(br, bc);

    // A box currently being evaluated higher in the recursion is treated as a wall.
    if (inStack[n1] || inStack[n2]) {
      return true;
    }
    // Pushing either way lands on a square from which no goal is reachable.
    if (dead[n1] && dead[n2]) {
      return true;
    }
    // A neighbour box that is itself frozen blocks this axis.
    if (occ[n1] && isFrozen(occ, n1)) {
      return true;
    }
    if (occ[n2] && isFrozen(occ, n2)) {
      return true;
    }
    return false;
  }
}
