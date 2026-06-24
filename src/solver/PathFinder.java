package solver;

import java.util.PriorityQueue;

public class PathFinder {
  private final Board b;
  private final int[] frontier;

  private final int[] reachStamp;
  private int reachCounter = 0;

  private final int[] cameFrom;
  private final int[] cameDir;
  private final int[] gCost;
  private final int[] pathStamp;
  private int pathCounter = 0;

  private static class AStarEntry implements Comparable<AStarEntry> {
    final int cell;
    final int f;
    AStarEntry(int cell, int f) { this.cell = cell; this.f = f; }
    @Override public int compareTo(AStarEntry o) { return Integer.compare(f, o.f); }
  }

  public PathFinder(Board b) {
    this.b = b;
    this.frontier = new int[b.numCells];
    this.reachStamp = new int[b.numCells];
    this.cameFrom = new int[b.numCells];
    this.cameDir = new int[b.numCells];
    this.gCost = new int[b.numCells];
    this.pathStamp = new int[b.numCells];
  }

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

  public boolean isReachable(int cell) {
    return reachStamp[cell] == reachCounter;
  }

  public void appendPath(StringBuilder sb, int start, int goal, boolean[] occ) {
    if (start == goal) {
      return;
    }
    pathCounter++;

    int goalR = b.rowOf(goal);
    int goalC = b.colOf(goal);

    PriorityQueue<AStarEntry> open = new PriorityQueue<>();
    gCost[start] = 0;
    pathStamp[start] = pathCounter;
    cameFrom[start] = -1;
    open.add(new AStarEntry(start, manhattan(start, goalR, goalC)));

    while (!open.isEmpty()) {
      int cur = open.poll().cell;
      if (cur == goal) {
        break;
      }
      int r = b.rowOf(cur);
      int c = b.colOf(cur);
      int ng = gCost[cur] + 1;
      for (int d = 0; d < 4; d++) {
        int nr = r + Board.DR[d];
        int nc = c + Board.DC[d];
        if (!b.inBounds(nr, nc)) {
          continue;
        }
        int nb = b.cellId(nr, nc);
        if (b.wall[nb] || occ[nb]) {
          continue;
        }
        if (pathStamp[nb] != pathCounter || ng < gCost[nb]) {
          pathStamp[nb] = pathCounter;
          gCost[nb] = ng;
          cameFrom[nb] = cur;
          cameDir[nb] = d;
          open.add(new AStarEntry(nb, ng + manhattan(nb, goalR, goalC)));
        }
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

  private int manhattan(int cell, int goalR, int goalC) {
    return Math.abs(b.rowOf(cell) - goalR) + Math.abs(b.colOf(cell) - goalC);
  }
}
