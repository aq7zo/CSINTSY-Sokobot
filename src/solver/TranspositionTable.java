package solver;

/**
 * Zobrist-hash-keyed visited table (Blueprint sections 3 and 5.4).
 *
 * One structure serves two jobs:
 *   - live states map to the best (lowest) g(n) seen so far, so re-encounters that are no cheaper
 *     are discarded;
 *   - states proven to be deadlocks are stored with the {@link #DEAD} marker, so any later path to
 *     the same board is rejected by an O(1) lookup before a Node is even constructed.
 */
public class TranspositionTable {
  /** Returned by {@link #getBestG} when the hash has never been recorded. */
  public static final int ABSENT = Integer.MAX_VALUE;
  /** Stored as the value for a known-dead state. */
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
