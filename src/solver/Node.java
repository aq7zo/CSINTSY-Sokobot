package solver;

public class Node implements Comparable<Node> {
  final int[] crates;
  final int playerNorm;
  final long hash;
  final int g;
  final int h;
  final int f;
  final Node parent;
  final int pushFrom;
  final int pushDir;
  final int pushCount;
  final int seq;

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
