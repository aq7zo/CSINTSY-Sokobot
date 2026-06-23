package solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Push-based weighted A* search (Blueprint sections 2, 3, 6).
 *
 * Branches on pushes (move one crate one tile), normalizes the player to its reachable region,
 * dedups states through a Zobrist transposition table, prunes deadlocks via static dead squares +
 * freeze checks + an infinite heuristic, and self-enforces a wall-clock deadline. Returns the first
 * complete solution found (greedy-leaning, since grading is pass/fail on a complete solve), or ""
 * if none is found within the budget / node cap.
 */
public class AStarSolver {
  // Weighted A*: f = g + (W_NUM/W_DEN) * h. Weight > 1 trades optimality for speed (section 6.2).
  private static final int W_NUM = 3;
  private static final int W_DEN = 2;
  // Power-of-two so the deadline check can use a bit mask.
  private static final int TIME_CHECK_INTERVAL = 2048;
  // Memory guard (section 6.4). Tunable; the time limit usually bites first on hard levels.
  private static final int NODE_CAP = 3_000_000;

  private final Board b;
  private final long deadline;
  private final DeadlockDetector deadlock;
  private final Heuristic heuristic;
  private final PathFinder pathFinder;
  private final TranspositionTable table;

  private final boolean[] occ;   // crate occupancy scratch, rebuilt per expansion
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
    int rootH = heuristic.compute(b.crateStart);
    clearOcc(b.crateStart);
    if (rootH >= Heuristic.INF) {
      return "";  // start is already a deadlock; unsolvable
    }

    PriorityQueue<Node> open = new PriorityQueue<>();
    int seq = 0;
    Node root = new Node(b.crateStart.clone(), playerNorm, rootHash,
        0, rootH, fValue(0, rootH), null, -1, -1, seq++);
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
        continue;  // a cheaper path to this state was found after it was queued
      }
      if (allOnTargets(node.crates)) {
        return reconstruct(node);  // backup goal test (normally caught at generation)
      }

      int[] crates = node.crates;
      for (int c : crates) {
        occ[c] = true;
      }
      pathFinder.reachRegion(node.playerNorm, occ);

      // Collect legal pushes using the parent's reachability before any successor BFS clobbers it.
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
            continue;  // blocked or static dead-square prune
          }
          int standId = b.cellId(standR, standC);
          if (b.wall[standId] || occ[standId] || !pathFinder.isReachable(standId)) {
            continue;  // player can't get behind the crate to push it
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

        // Simulate the push: crate cell -> destId, player ends on the old crate cell.
        occ[cell] = false;
        occ[destId] = true;
        int newNorm = pathFinder.reachRegion(cell, occ);
        long newHash = node.hash
            ^ b.zCrate[cell] ^ b.zCrate[destId]
            ^ b.zPlayer[node.playerNorm] ^ b.zPlayer[newNorm];
        int gNew = node.g + 1;

        boolean keep = false;
        if (!table.isDead(newHash)) {
          int seen = table.getBestG(newHash);
          if (seen == TranspositionTable.ABSENT || seen > gNew) {
            if (deadlock.isFreezeDeadlock(occ, destId)) {
              table.markDead(newHash);
            } else {
              keep = true;
            }
          }
        }

        if (keep) {
          int[] newCrates = crates.clone();
          newCrates[candIdx[i]] = destId;
          if (allOnTargets(newCrates)) {
            occ[cell] = true;
            occ[destId] = false;
            Node goalNode = new Node(newCrates, newNorm, newHash,
                gNew, 0, gNew, node, cell, d, seq++);
            return reconstruct(goalNode);
          }
          int hNew = heuristic.compute(newCrates);
          if (hNew >= Heuristic.INF) {
            table.markDead(newHash);
          } else {
            table.put(newHash, gNew);
            open.add(new Node(newCrates, newNorm, newHash,
                gNew, hNew, fValue(gNew, hNew), node, cell, d, seq++));
          }
        }

        // Revert the simulated push.
        occ[cell] = true;
        occ[destId] = false;
      }

      // Reset occupancy for the next node.
      for (int c : crates) {
        occ[c] = false;
      }
    }

    return "";  // no complete solution within budget; partial paths earn nothing (section 6.2/6.3)
  }

  private int fValue(int g, int h) {
    return g + (W_NUM * h) / W_DEN;
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

  /**
   * Replay the push chain from the start, walking the player to each standing square and emitting
   * the player path followed by the push step. Produces the primitive u/d/l/r string.
   */
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
      int stand = b.cellId(b.rowOf(from) - Board.DR[d], b.colOf(from) - Board.DC[d]);
      int dest = b.cellId(b.rowOf(from) + Board.DR[d], b.colOf(from) + Board.DC[d]);
      pathFinder.appendPath(sb, player, stand, rocc);
      sb.append(Board.DCHAR[d]);
      rocc[from] = false;
      rocc[dest] = true;
      player = from;
    }
    return sb.toString();
  }
}
