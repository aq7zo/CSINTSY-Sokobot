package solver;

public class LongIntMap {
  private long[] keys;
  private int[] values;
  private boolean[] used;
  private int size;
  private int mask;
  private int threshold;

  public LongIntMap(int initialCapacity) {
    int cap = 16;
    while (cap < initialCapacity) {
      cap <<= 1;
    }
    keys = new long[cap];
    values = new int[cap];
    used = new boolean[cap];
    mask = cap - 1;
    threshold = (int) (cap * 0.6);
  }

  public int size() {
    return size;
  }

  private static int mix(long k) {
    k ^= (k >>> 33);
    k *= 0xff51afd7ed558ccdL;
    k ^= (k >>> 33);
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= (k >>> 33);
    return (int) k;
  }

  public int getOrDefault(long key, int def) {
    int i = mix(key) & mask;
    while (used[i]) {
      if (keys[i] == key) {
        return values[i];
      }
      i = (i + 1) & mask;
    }
    return def;
  }

  public void put(long key, int value) {
    int i = mix(key) & mask;
    while (used[i]) {
      if (keys[i] == key) {
        values[i] = value;
        return;
      }
      i = (i + 1) & mask;
    }
    used[i] = true;
    keys[i] = key;
    values[i] = value;
    size++;
    if (size >= threshold) {
      resize();
    }
  }

  private void resize() {
    long[] oldKeys = keys;
    int[] oldValues = values;
    boolean[] oldUsed = used;
    int newCap = keys.length << 1;
    keys = new long[newCap];
    values = new int[newCap];
    used = new boolean[newCap];
    mask = newCap - 1;
    threshold = (int) (newCap * 0.6);
    size = 0;
    for (int j = 0; j < oldKeys.length; j++) {
      if (oldUsed[j]) {
        put(oldKeys[j], oldValues[j]);
      }
    }
  }
}
