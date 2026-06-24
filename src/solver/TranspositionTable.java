package solver;

public class TranspositionTable {
  public static final int ABSENT = Integer.MAX_VALUE;
  private static final int DEAD = Integer.MIN_VALUE;

  private final LongIntMap map;

  public TranspositionTable(int initialCapacity) {
    this.map = new LongIntMap(initialCapacity);
  }

  public int getBestG(long hash) {
    return map.getOrDefault(hash, ABSENT);
  }

  public void put(long hash, int g) {
    map.put(hash, g);
  }

  public boolean isDead(long hash) {
    return map.getOrDefault(hash, ABSENT) == DEAD;
  }

  public void markDead(long hash) {
    map.put(hash, DEAD);
  }

  public int size() {
    return map.size();
  }
}
