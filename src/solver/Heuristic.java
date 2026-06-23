package solver;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Heuristic h(n) (Blueprint section 4): a wall-aware lower bound on the number of pushes left.
 *
 * Preprocessing builds, for every target, a push-distance map via reverse BFS (the minimum pushes
 * to move a single crate from any cell to that target, ignoring other crates). Per state, h(n) is
 * the minimum-cost perfect matching of crates to targets under those distances (Hungarian
 * algorithm). The matching of true single-crate push distances is an admissible lower bound on the
 * total pushes required, so with weight 1 A* is push-optimal; the solver applies a weight > 1 for
 * speed (Blueprint section 6).
 *
 * If no finite perfect matching exists (some crate can reach no available target), h(n) is INF,
 * which marks the state as a deadlock (section 5.4).
 */
public class Heuristic {
  /** Sentinel for "infinite" heuristic (deadlock). Small enough that g + INF cannot overflow. */
  public static final int INF = Integer.MAX_VALUE / 4;

  private static final int DIST_INF = Integer.MAX_VALUE / 4;
  /** Cost assigned to an unreachable crate/target pair; far above any real matching total. */
  private static final int BIG = 1_000_000;

  private final Board b;
  private final int n;             // number of targets (== number of crates)
  private final int[][] pushDist;  // [targetIndex][cellId] -> min pushes, or DIST_INF

  // Hungarian algorithm scratch buffers (1-indexed, reused across calls).
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
  }

  /** Reverse-BFS push distances from one target to every cell (ignoring other crates). */
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
        int ar = cr - Board.DR[d];      // crate's previous cell
        int ac = cc - Board.DC[d];
        int pr = cr - 2 * Board.DR[d];  // player's standing cell
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

  /**
   * Minimum-cost matching of the given crates to targets. Returns {@link #INF} if no crate-to-target
   * assignment with finite cost exists (deadlock).
   */
  public int compute(int[] crateCells) {
    for (int i = 1; i <= n; i++) {
      int crateCell = crateCells[i - 1];
      for (int j = 1; j <= n; j++) {
        int d = pushDist[j - 1][crateCell];
        cost[i][j] = (d >= DIST_INF) ? BIG : d;
      }
    }
    int total = hungarian();
    return total >= BIG ? INF : total;
  }

  /**
   * Classic O(n^3) Hungarian algorithm for the assignment problem (minimization), 1-indexed.
   * Standard potentials-and-augmenting-path formulation. Returns the minimum total assignment cost.
   */
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
