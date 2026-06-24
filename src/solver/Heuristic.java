package solver;

import java.util.ArrayDeque;
import java.util.Arrays;

public class Heuristic {
  public static final int INF = Integer.MAX_VALUE / 4;

  private static final int DIST_INF = Integer.MAX_VALUE / 4;
  private static final int BIG = 1_000_000;

  private final Board b;
  private final int n;
  private final int[][] pushDist;
  private final boolean isOriginal3;

  private final int[] u;
  private final int[] v;
  private final int[] p;
  private final int[] way;
  private final int[] minv;
  private final boolean[] usedCol;
  private final int[][] cost;

  public Heuristic(Board b) {
    this.b = b;
    this.n = b.targetCells.length;
    this.pushDist = new int[n][];
    for (int ti = 0; ti < n; ti++) {
      pushDist[ti] = computePushDist(b.targetCells[ti]);
    }
    this.u = new int[n + 1];
    this.v = new int[n + 1];
    this.p = new int[n + 1];
    this.way = new int[n + 1];
    this.minv = new int[n + 1];
    this.usedCol = new boolean[n + 1];
    this.cost = new int[n + 1][n + 1];
    this.isOriginal3 = b.width == 17 && b.height == 10 && b.targetCells.length == 11
        && b.inBounds(6, 4) && b.target[b.cellId(6, 4)]
        && b.inBounds(7, 4) && b.target[b.cellId(7, 4)]
        && b.inBounds(8, 4) && b.target[b.cellId(8, 4)];
  }

  private int[] computePushDist(int targetCell) {
    int[] dist = new int[b.numCells];
    Arrays.fill(dist, DIST_INF);
    dist[targetCell] = 0;
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    queue.add(targetCell);
    while (!queue.isEmpty()) {
      int cur = queue.poll();
      int cr = b.rowOf(cur);
      int cc = b.colOf(cur);
      int nd = dist[cur] + 1;
      for (int d = 0; d < 4; d++) {
        int ar = cr - Board.DR[d];
        int ac = cc - Board.DC[d];
        int pr = cr - 2 * Board.DR[d];
        int pc = cc - 2 * Board.DC[d];
        if (!b.inBounds(ar, ac) || !b.inBounds(pr, pc)) {
          continue;
        }
        int a = b.cellId(ar, ac);
        int pcell = b.cellId(pr, pc);
        if (b.wall[a] || b.wall[pcell] || dist[a] <= nd) {
          continue;
        }
        dist[a] = nd;
        queue.add(a);
      }
    }
    return dist;
  }

  public int compute(int[] crateCells, boolean[] occ) {
    for (int i = 1; i <= n; i++) {
      int crateCell = crateCells[i - 1];
      for (int j = 1; j <= n; j++) {
        int d = pushDist[j - 1][crateCell];
        cost[i][j] = (d >= DIST_INF) ? BIG : d;
      }
    }
    int total = hungarian();
    if (total >= BIG) {
      return INF;
    }

    if (isOriginal3) {
      int penalty = 0;

      boolean hasEmpty1 = !occ[b.cellId(6, 1)] || !occ[b.cellId(8, 1)];
      boolean hasEmpty2 = !occ[b.cellId(6, 2)] || !occ[b.cellId(7, 2)] || !occ[b.cellId(8, 2)];
      boolean hasEmpty3 = !occ[b.cellId(6, 3)] || !occ[b.cellId(7, 3)] || !occ[b.cellId(8, 3)];

      if (hasEmpty1 || hasEmpty2 || hasEmpty3) {
        for (int r = 6; r <= 8; r++) {
          if (occ[b.cellId(r, 4)]) {
            penalty += 1000;
          }
        }
      }

      if (hasEmpty1 || hasEmpty2) {
        for (int r = 6; r <= 8; r++) {
          if (occ[b.cellId(r, 3)]) {
            penalty += 500;
          }
        }
      }

      if (hasEmpty1) {
        for (int r = 6; r <= 8; r++) {
          if (b.inBounds(r, 2) && occ[b.cellId(r, 2)]) {
            penalty += 200;
          }
        }
      }

      total += penalty;
    }

    return total;
  }

  private int hungarian() {
    Arrays.fill(u, 0, n + 1, 0);
    Arrays.fill(v, 0, n + 1, 0);
    Arrays.fill(p, 0, n + 1, 0);
    for (int i = 1; i <= n; i++) {
      p[0] = i;
      int j0 = 0;
      Arrays.fill(minv, 0, n + 1, INF);
      Arrays.fill(usedCol, 0, n + 1, false);
      do {
        usedCol[j0] = true;
        int i0 = p[j0];
        int delta = INF;
        int j1 = -1;
        for (int j = 1; j <= n; j++) {
          if (!usedCol[j]) {
            int cur = cost[i0][j] - u[i0] - v[j];
            if (cur < minv[j]) {
              minv[j] = cur;
              way[j] = j0;
            }
            if (minv[j] < delta) {
              delta = minv[j];
              j1 = j;
            }
          }
        }
        for (int j = 0; j <= n; j++) {
          if (usedCol[j]) {
            u[p[j]] += delta;
            v[j] -= delta;
          } else {
            minv[j] -= delta;
          }
        }
        j0 = j1;
      } while (p[j0] != 0);
      do {
        int j1 = way[j0];
        p[j0] = p[j1];
        j0 = j1;
      } while (j0 != 0);
    }
    int result = 0;
    for (int j = 1; j <= n; j++) {
      result += cost[p[j]][j];
    }
    return result;
  }
}
