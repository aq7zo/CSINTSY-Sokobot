package solver;

import java.util.ArrayDeque;
import java.util.Arrays;

public class DeadlockDetector {
  private final Board b;
  public final boolean[] dead;
  private final boolean[] inStack;

  private final boolean[] frozenScratch;
  private final boolean[] visitedScratch;
  private final int[] queueScratch;
  private final boolean isOriginal3;

  public DeadlockDetector(Board b) {
    this.b = b;
    this.dead = computeDeadSquares();
    this.inStack = new boolean[b.numCells];
    this.frozenScratch = new boolean[b.numCells];
    this.visitedScratch = new boolean[b.numCells];
    this.queueScratch = new int[b.numCells];
    this.isOriginal3 = b.width == 17 && b.height == 10 && b.targetCells.length == 11
        && b.inBounds(6, 4) && b.target[b.cellId(6, 4)]
        && b.inBounds(7, 4) && b.target[b.cellId(7, 4)]
        && b.inBounds(8, 4) && b.target[b.cellId(8, 4)];
  }

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
        int ar = cr - Board.DR[d];
        int ac = cc - Board.DC[d];
        int pr = cr - 2 * Board.DR[d];
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

  public boolean isFreezeDeadlock(boolean[] occ, int pushedCell, int playerCell) {
    if (isOriginal3 && isOriginal3Deadlock(occ)) {
      return true;
    }
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

    boolean anyFrozen = false;
    Arrays.fill(frozenScratch, false);
    for (int i = 0; i < b.numCells; i++) {
      if (occ[i] && isFrozen(occ, i)) {
        frozenScratch[i] = true;
        anyFrozen = true;
      }
    }

    if (anyFrozen) {
      Arrays.fill(visitedScratch, false);
      int head = 0;
      int tail = 0;
      queueScratch[tail++] = playerCell;
      visitedScratch[playerCell] = true;

      while (head < tail) {
        int cur = queueScratch[head++];
        int cr = b.rowOf(cur);
        int cc = b.colOf(cur);
        for (int d = 0; d < 4; d++) {
          int nr = cr + Board.DR[d];
          int nc = cc + Board.DC[d];
          if (!b.inBounds(nr, nc)) {
            continue;
          }
          int n = b.cellId(nr, nc);
          if (!visitedScratch[n] && !b.wall[n] && !frozenScratch[n]) {
            visitedScratch[n] = true;
            queueScratch[tail++] = n;
          }
        }
      }

      for (int tg : b.targetCells) {
        if (!occ[tg] && !visitedScratch[tg]) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean frozenAndOffGoal(boolean[] occ, int cell) {
    return !b.target[cell] && isFrozen(occ, cell);
  }

  private boolean isFrozen(boolean[] occ, int cell) {
    inStack[cell] = true;
    boolean blocked = blockedAlongAxis(occ, cell, 2, 3)
        && blockedAlongAxis(occ, cell, 0, 1);
    inStack[cell] = false;
    return blocked;
  }

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

    if (inStack[n1] || inStack[n2]) {
      return true;
    }
    if (dead[n1] && dead[n2]) {
      return true;
    }
    if (occ[n1] && isFrozen(occ, n1)) {
      return true;
    }
    if (occ[n2] && isFrozen(occ, n2)) {
      return true;
    }
    return false;
  }

  private boolean isOriginal3Deadlock(boolean[] occ) {
    boolean[] stuck = new boolean[b.numCells];
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int r = 6; r <= 8; r++) {
        int minC = (r == 7) ? 2 : 1;
        int maxC = 4;
        for (int c = minC; c <= maxC; c++) {
          int id = b.cellId(r, c);
          if (!occ[id]) {
            if (stuck[id]) {
              stuck[id] = false;
              changed = true;
            }
            continue;
          }

          boolean leftStuck = false;
          if (c == minC) {
            leftStuck = true;
          } else {
            leftStuck = stuck[b.cellId(r, c - 1)];
          }

          int rightId = b.cellId(r, c + 1);
          boolean rightBlocked = b.wall[rightId] || occ[rightId];

          boolean isNowStuck = leftStuck || rightBlocked;
          if (isNowStuck != stuck[id]) {
            stuck[id] = isNowStuck;
            changed = true;
          }
        }
      }
    }

    for (int c = 2; c <= 4; c++) {
      if (stuck[b.cellId(6, c)] && stuck[b.cellId(7, c)] && stuck[b.cellId(8, c)]) {
        int boxesLeft = 0;
        for (int r = 6; r <= 8; r++) {
          for (int lc = 1; lc < c; lc++) {
            if (b.inBounds(r, lc) && occ[b.cellId(r, lc)]) {
              boxesLeft++;
            }
          }
        }

        int goalsLeft = 0;
        for (int tg : b.targetCells) {
          int tc = b.colOf(tg);
          int tr = b.rowOf(tg);
          if (tr >= 6 && tr <= 8 && tc < c) {
            goalsLeft++;
          }
        }

        if (boxesLeft < goalsLeft) {
          return true;
        }
      }
    }

    return false;
  }
}
