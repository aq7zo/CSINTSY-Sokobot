package solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;

public class AStarSolver {
  private static final int W_NUM = 3;
  private static final int W_DEN = 2;
  private static final int TIME_CHECK_INTERVAL = 2048;
  private static final int NODE_CAP = 3_000_000;

  private final Board b;
  private final long deadline;
  private final DeadlockDetector deadlock;
  private final Heuristic heuristic;
  private final PathFinder pathFinder;
  private final TranspositionTable table;

  private final boolean[] occ;
  private long expansions = 0;

  public AStarSolver(Board b, long deadlineNanos) {
    this.b = b;
    this.deadline = deadlineNanos;
    this.deadlock = new DeadlockDetector(b);
    this.heuristic = new Heuristic(b);
    this.pathFinder = new PathFinder(b);
    this.table = new TranspositionTable(1 << 16);
    this.occ = new boolean[b.numCells];
  }

  public String solve() {
    int k = b.crateStart.length;
    for (int c : b.crateStart) {
      occ[c] = true;
    }
    if (allOnTargets(b.crateStart)) {
      return "";
    }
    int playerNorm = pathFinder.reachRegion(b.playerStart, occ);
    long rootHash = zobrist(b.crateStart, playerNorm);
    int rootH = heuristic.compute(b.crateStart, occ);
    clearOcc(b.crateStart);
    if (rootH >= Heuristic.INF) {
      return "";
    }

    PriorityQueue<Node> open = new PriorityQueue<>();
    int seq = 0;
    Node root = new Node(b.crateStart.clone(), playerNorm, rootHash,
        0, rootH, fValue(0, rootH), null, -1, -1, 0, seq++);
    open.add(root);
    table.put(rootHash, 0);

    int[] candFrom = new int[4 * k];
    int[] candIdx = new int[4 * k];
    int[] candDir = new int[4 * k];

    while (!open.isEmpty()) {
      if ((++expansions & (TIME_CHECK_INTERVAL - 1)) == 0 && System.nanoTime() >= deadline) {
        break;
      }
      if (table.size() > NODE_CAP) {
        break;
      }

      Node node = open.poll();
      int best = table.getBestG(node.hash);
      if (best != TranspositionTable.ABSENT && best < node.g) {
        continue;
      }
      if (allOnTargets(node.crates)) {
        return reconstruct(node);
      }

      int[] crates = node.crates;
      for (int c : crates) {
        occ[c] = true;
      }
      pathFinder.reachRegion(node.playerNorm, occ);

      int nc = 0;
      for (int ci = 0; ci < crates.length; ci++) {
        int cell = crates[ci];
        int cr = b.rowOf(cell);
        int cc = b.colOf(cell);
        for (int d = 0; d < 4; d++) {
          int destR = cr + Board.DR[d];
          int destC = cc + Board.DC[d];
          int standR = cr - Board.DR[d];
          int standC = cc - Board.DC[d];
          if (!b.inBounds(destR, destC) || !b.inBounds(standR, standC)) {
            continue;
          }
          int destId = b.cellId(destR, destC);
          if (b.wall[destId] || occ[destId] || deadlock.dead[destId]) {
            continue;
          }
          int standId = b.cellId(standR, standC);
          if (b.wall[standId] || occ[standId] || !pathFinder.isReachable(standId)) {
            continue;
          }
          candFrom[nc] = cell;
          candIdx[nc] = ci;
          candDir[nc] = d;
          nc++;
        }
      }

      for (int i = 0; i < nc; i++) {
        int cell = candFrom[i];
        int d = candDir[i];
        int destId = b.cellId(b.rowOf(cell) + Board.DR[d], b.colOf(cell) + Board.DC[d]);

        int endId = tunnelPush(destId, d);
        int pushCount = Math.abs(b.rowOf(endId) - b.rowOf(cell))
            + Math.abs(b.colOf(endId) - b.colOf(cell));

        occ[cell] = false;
        occ[endId] = true;
        int newNorm = pathFinder.reachRegion(cell, occ);
        long newHash = node.hash
            ^ b.zCrate[cell] ^ b.zCrate[endId]
            ^ b.zPlayer[node.playerNorm] ^ b.zPlayer[newNorm];
        int gNew = node.g + pushCount;

        boolean keep = false;
        if (!table.isDead(newHash)) {
          int seen = table.getBestG(newHash);
          if (seen == TranspositionTable.ABSENT || seen > gNew) {
            if (deadlock.isFreezeDeadlock(occ, endId, newNorm)) {
              table.markDead(newHash);
            } else {
              keep = true;
            }
          }
        }

        if (keep) {
          int[] newCrates = crates.clone();
          newCrates[candIdx[i]] = endId;
          if (allOnTargets(newCrates)) {
            occ[cell] = true;
            occ[endId] = false;
            Node goalNode = new Node(newCrates, newNorm, newHash,
                gNew, 0, gNew, node, cell, d, pushCount, seq++);
            return reconstruct(goalNode);
          }
          int hNew = heuristic.compute(newCrates, occ);
          if (hNew >= Heuristic.INF) {
            table.markDead(newHash);
          } else {
            table.put(newHash, gNew);
            open.add(new Node(newCrates, newNorm, newHash,
                gNew, hNew, fValue(gNew, hNew), node, cell, d, pushCount, seq++));
          }
        }

        occ[cell] = true;
        occ[endId] = false;
      }

      for (int c : crates) {
        occ[c] = false;
      }
    }

    return "";
  }

  private int fValue(int g, int h) {
    return g + (W_NUM * h) / W_DEN;
  }

  private int tunnelPush(int destId, int d) {
    int end = destId;
    while (!b.target[end] && isTunnel(end, d)) {
      int nr = b.rowOf(end) + Board.DR[d];
      int nc = b.colOf(end) + Board.DC[d];
      if (!b.inBounds(nr, nc)) {
        break;
      }
      int next = b.cellId(nr, nc);
      if (b.wall[next] || occ[next] || deadlock.dead[next]) {
        break;
      }
      if (!isTunnel(next, d)) {
        break;
      }
      end = next;
    }
    return end;
  }

  private boolean isTunnel(int cell, int d) {
    int r = b.rowOf(cell);
    int c = b.colOf(cell);
    if (d <= 1) {
      return wallAt(r, c - 1) && wallAt(r, c + 1);
    }
    return wallAt(r - 1, c) && wallAt(r + 1, c);
  }

  private boolean wallAt(int r, int c) {
    return !b.inBounds(r, c) || b.wall[b.cellId(r, c)];
  }

  private boolean allOnTargets(int[] crates) {
    for (int c : crates) {
      if (!b.target[c]) {
        return false;
      }
    }
    return true;
  }

  private long zobrist(int[] crates, int playerNorm) {
    long h = b.zPlayer[playerNorm];
    for (int c : crates) {
      h ^= b.zCrate[c];
    }
    return h;
  }

  private void clearOcc(int[] crates) {
    for (int c : crates) {
      occ[c] = false;
    }
  }

  private String reconstruct(Node goalNode) {
    ArrayList<Node> pushes = new ArrayList<>();
    for (Node n = goalNode; n != null && n.pushDir >= 0; n = n.parent) {
      pushes.add(n);
    }
    Collections.reverse(pushes);

    boolean[] rocc = new boolean[b.numCells];
    for (int c : b.crateStart) {
      rocc[c] = true;
    }
    int player = b.playerStart;
    StringBuilder sb = new StringBuilder();
    for (Node n : pushes) {
      int from = n.pushFrom;
      int d = n.pushDir;
      int count = n.pushCount;
      int dr = Board.DR[d];
      int dc = Board.DC[d];
      int fr = b.rowOf(from);
      int fc = b.colOf(from);
      int stand = b.cellId(fr - dr, fc - dc);
      pathFinder.appendPath(sb, player, stand, rocc);
      for (int s = 0; s < count; s++) {
        sb.append(Board.DCHAR[d]);
      }
      int dest = b.cellId(fr + dr * count, fc + dc * count);
      rocc[from] = false;
      rocc[dest] = true;
      player = b.cellId(fr + dr * (count - 1), fc + dc * (count - 1));
    }
    return sb.toString();
  }
}
