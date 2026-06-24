package solver;

import java.util.ArrayList;
import java.util.Random;

public class Board {

  public static final int[] DR = {-1, 1, 0, 0};
  public static final int[] DC = {0, 0, -1, 1};
  public static final char[] DCHAR = {'u', 'd', 'l', 'r'};

  public final int width;
  public final int height;
  public final int numCells;

  public final boolean[] wall;
  public final boolean[] target;
  public final int[] targetCells;

  public final int playerStart;
  public final int[] crateStart;

  public final long[] zCrate;
  public final long[] zPlayer;

  public Board(int width, int height, char[][] mapData, char[][] itemsData) {
    this.width = width;
    this.height = height;
    this.numCells = width * height;
    this.wall = new boolean[numCells];
    this.target = new boolean[numCells];

    int player = -1;
    ArrayList<Integer> targets = new ArrayList<>();
    ArrayList<Integer> crates = new ArrayList<>();

    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        int id = r * width + c;
        char m = charAt(mapData, r, c);
        char it = charAt(itemsData, r, c);
        if (m == '#') {
          wall[id] = true;
        } else if (m == '.') {
          target[id] = true;
          targets.add(id);
        }
        if (it == '@') {
          player = id;
        } else if (it == '$') {
          crates.add(id);
        }
      }
    }

    this.playerStart = player;
    this.targetCells = toArray(targets);
    this.crateStart = toArray(crates);

    Random rng = new Random(0x9E3779B97F4A7C15L);
    this.zCrate = new long[numCells];
    this.zPlayer = new long[numCells];
    for (int i = 0; i < numCells; i++) {
      zCrate[i] = rng.nextLong();
      zPlayer[i] = rng.nextLong();
    }
  }

  public int cellId(int r, int c) {
    return r * width + c;
  }

  public int rowOf(int id) {
    return id / width;
  }

  public int colOf(int id) {
    return id % width;
  }

  public boolean inBounds(int r, int c) {
    return r >= 0 && r < height && c >= 0 && c < width;
  }

  private static char charAt(char[][] grid, int r, int c) {
    if (grid == null || r < 0 || r >= grid.length) {
      return ' ';
    }
    char[] row = grid[r];
    if (row == null || c < 0 || c >= row.length) {
      return ' ';
    }
    return row[c];
  }

  private static int[] toArray(ArrayList<Integer> list) {
    int[] out = new int[list.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = list.get(i);
    }
    return out;
  }
}
