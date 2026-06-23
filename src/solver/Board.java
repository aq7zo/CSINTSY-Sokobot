package solver;

import java.util.ArrayList;
import java.util.Random;

/**
 * Immutable static board model + Zobrist key tables + direction constants
 * (Blueprint sections 1 and 7).
 *
 * Coordinate convention, grounded in the starter code (GamePanel -> BotThread -> SokoBot):
 *   width  = number of columns, height = number of rows, arrays indexed [row][col],
 *   cellId = row * width + col.
 * Move chars (from GamePanel.executeMove): 'u'=row-1, 'd'=row+1, 'l'=col-1, 'r'=col+1.
 *
 * mapData carries only '#'(wall) and '.'(target); itemsData carries only '@'(player) and
 * '$'(crate). Crates may already sit on targets, and the player may start on a target.
 */
public class Board {

  /** Direction order: 0=up, 1=down, 2=left, 3=right. */
  public static final int[] DR = {-1, 1, 0, 0};
  public static final int[] DC = {0, 0, -1, 1};
  public static final char[] DCHAR = {'u', 'd', 'l', 'r'};

  public final int width;
  public final int height;
  public final int numCells;

  public final boolean[] wall;     // indexed by cellId
  public final boolean[] target;   // indexed by cellId
  public final int[] targetCells;  // cellIds holding a target

  public final int playerStart;    // cellId of the player ('@'), or -1 if absent
  public final int[] crateStart;   // cellIds of initial crates ('$')

  public final long[] zCrate;      // Zobrist key: a crate occupies this cell
  public final long[] zPlayer;     // Zobrist key: the normalized player occupies this cell

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

    // Fixed seed so runs are reproducible while debugging.
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
