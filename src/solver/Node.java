package solver;

/**
 * A search node (Blueprint section 7). To keep memory down across millions of nodes, the canonical
 * state (crate cells + normalized player + Zobrist hash) is folded directly into the node rather
 * than living in a separate GameState object — see CHECKLIST.md for this deviation from the
 * blueprint's class list.
 *
 * The push that produced this node is recorded as (pushFrom, pushDir, pushCount): the crate that
 * moved started at cell {@code pushFrom} and was pushed {@code pushCount} times in direction
 * {@code pushDir} (pushCount > 1 is a tunnel macro). The root node uses pushDir = -1.
 */
public class Node implements Comparable<Node> {
  final int[] crates;     // crate cell ids (unordered; Zobrist XOR is order-independent)
  final int playerNorm;   // normalized player cell (min reachable cell id)
  final long hash;        // Zobrist hash of (crates, playerNorm)
  final int g;            // pushes from start
  final int h;            // heuristic estimate of remaining pushes
  final int f;            // priority key (weighted: g + W*h)
  final Node parent;
  final int pushFrom;     // crate cell before the push that created this node
  final int pushDir;      // direction of that push, or -1 for the root
  final int pushCount;    // consecutive pushes in pushDir (tunnel macro); 1 for a plain push
  final int seq;          // insertion order, for deterministic tie-breaking

  Node(int[] crates, int playerNorm, long hash, int g, int h, int f,
       Node parent, int pushFrom, int pushDir, int pushCount, int seq) {
    this.crates = crates;
    this.playerNorm = playerNorm;
    this.hash = hash;
    this.g = g;
    this.h = h;
    this.f = f;
    this.parent = parent;
    this.pushFrom = pushFrom;
    this.pushDir = pushDir;
    this.pushCount = pushCount;
    this.seq = seq;
  }

  /** Order by f, then prefer smaller h (greedy-first among equals), then FIFO for determinism. */
  @Override
  public int compareTo(Node o) {
    if (f != o.f) {
      return Integer.compare(f, o.f);
    }
    if (h != o.h) {
      return Integer.compare(h, o.h);
    }
    return Integer.compare(seq, o.seq);
  }
}
