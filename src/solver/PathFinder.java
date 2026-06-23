package solver;

/**
 * BFS helpers over free cells (Blueprint sections 1.2, 2.2, 2.4). Used during search to compute the
 * player's reachable region (and its normalized access point) and during final move-string
 * reconstruction to walk the player to each push's standing square.
 *
 * Buffers are preallocated and reused; reachability uses a stamp counter to avoid clearing arrays.
 * Search (reachRegion) and reconstruction (appendPath) run in separate phases, so sharing the
 * frontier array between them is safe.
 */
public class PathFinder {
  private final Board b;
  private final int[] frontier;

  // Reachability state.
  private final int[] reachStamp;
  private int reachCounter = 0;

  // Path-reconstruction state.
  private final int[] cameFrom;
  private final int[] cameDir;
  private final int[] pathStamp;
  private int pathCounter = 0;

  public PathFinder(Board b) {
    this.b = b;
    this.frontier = new int[b.numCells];
    this.reachStamp = new int[b.numCells];
    this.cameFrom = new int[b.numCells];
    this.cameDir = new int[b.numCells];
    this.pathStamp = new int[b.numCells];
  }

  /**
   * Flood-fill from {@code start} over cells that are neither walls nor occupied by a crate
   * ({@code occ}). Marks reachable cells in the current stamp (queryable via {@link #isReachable})
   * and returns the normalized player position: the minimum reachable cell id.
   */
  public int reachRegion(int start, boolean[] occ) {
    reachCounter++;
    int head = 0;
    int tail = 0;
    frontier[tail++] = start;
    reachStamp[start] = reachCounter;
    int min = start;
    while (head < tail) {
      int cur = frontier[head++];
      if (cur < min) {
        min = cur;
      }
      int r = b.rowOf(cur);
      int c = b.colOf(cur);
      for (int d = 0; d < 4; d++) {
        int nr = r + Board.DR[d];
        int nc = c + Board.DC[d];
        if (!b.inBounds(nr, nc)) {
          continue;
        }
        int nb = b.cellId(nr, nc);
        if (reachStamp[nb] == reachCounter || b.wall[nb] || occ[nb]) {
          continue;
        }
        reachStamp[nb] = reachCounter;
        frontier[tail++] = nb;
      }
    }
    return min;
  }

  /** Whether {@code cell} was reachable in the most recent {@link #reachRegion} call. */
  public boolean isReachable(int cell) {
    return reachStamp[cell] == reachCounter;
  }

  /**
   * Append to {@code sb} the u/d/l/r moves that walk the player from {@code start} to {@code goal}
   * over free cells (neither walls nor crates in {@code occ}). The caller guarantees a path exists
   * (the push was validated as reachable during search); if not, throws to fail safe.
   */
  public void appendPath(StringBuilder sb, int start, int goal, boolean[] occ) {
    if (start == goal) {
      return;
    }
    pathCounter++;
    int head = 0;
    int tail = 0;
    frontier[tail++] = start;
    pathStamp[start] = pathCounter;
    cameFrom[start] = -1;
    while (head < tail) {
      int cur = frontier[head++];
      if (cur == goal) {
        break;
      }
      int r = b.rowOf(cur);
      int c = b.colOf(cur);
      for (int d = 0; d < 4; d++) {
        int nr = r + Board.DR[d];
        int nc = c + Board.DC[d];
        if (!b.inBounds(nr, nc)) {
          continue;
        }
        int nb = b.cellId(nr, nc);
        if (pathStamp[nb] == pathCounter || b.wall[nb] || occ[nb]) {
          continue;
        }
        pathStamp[nb] = pathCounter;
        cameFrom[nb] = cur;
        cameDir[nb] = d;
        frontier[tail++] = nb;
      }
    }
    if (pathStamp[goal] != pathCounter) {
      throw new IllegalStateException("reconstruction: standing square unreachable");
    }
    int len = 0;
    for (int cur = goal; cur != start; cur = cameFrom[cur]) {
      len++;
    }
    char[] tmp = new char[len];
    int cur = goal;
    for (int i = len - 1; i >= 0; i--) {
      tmp[i] = Board.DCHAR[cameDir[cur]];
      cur = cameFrom[cur];
    }
    sb.append(tmp);
  }
}
