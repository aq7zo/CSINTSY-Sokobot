package solver;

import java.io.File;
import java.util.Arrays;
import reader.FileReader;
import reader.MapData;

public class Harness {

  public static void main(String[] args) {
    String[] names = args.length > 0 ? args : listMaps();
    int solved = 0;
    double totalTime = 0;
    for (String name : names) {
      Result r = runOne(name);
      if (r == null) {
        System.out.printf("%-14s  ERROR (could not load)%n", name);
        continue;
      }
      totalTime += r.seconds;
      if (r.solved) {
        solved++;
      }
      System.out.printf("%-14s  %-4s  %6.2fs  crates=%d  moves=%d%n",
          name, r.solved ? "PASS" : "FAIL", r.seconds, r.crates, r.moveCount);
    }
    System.out.printf("%n%d/%d solved   (%.1f%%)   totalTime=%.2fs%n",
        solved, names.length, 100.0 * solved / names.length, totalTime);
  }

  private static class Result {
    boolean solved;
    double seconds;
    int moveCount;
    int crates;
  }

  private static Result runOne(String name) {
    FileReader reader = new FileReader();
    MapData md = reader.readFile(name);
    if (md == null) {
      return null;
    }
    int rows = md.rows;
    int cols = md.columns;
    char[][] map = new char[rows][cols];
    char[][] items = new char[rows][cols];
    int crateCount = 0;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        char t = md.tiles[i][j];
        char m = ' ';
        char it = ' ';
        switch (t) {
          case '#': m = '#'; break;
          case '.': m = '.'; break;
          case '@': it = '@'; break;
          case '$': it = '$'; crateCount++; break;
          case '+': m = '.'; it = '@'; break;
          case '*': m = '.'; it = '$'; crateCount++; break;
          default: break;
        }
        map[i][j] = m;
        items[i][j] = it;
      }
    }

    SokoBot bot = new SokoBot();
    long t0 = System.nanoTime();
    String sol = bot.solveSokobanPuzzle(cols, rows, deepCopy(map), deepCopy(items));
    long t1 = System.nanoTime();

    Result r = new Result();
    r.seconds = (t1 - t0) / 1e9;
    r.moveCount = sol == null ? 0 : sol.length();
    r.crates = crateCount;
    r.solved = sol != null && simulate(map, items, sol);
    return r;
  }

  private static boolean simulate(char[][] map, char[][] items, String sol) {
    int rows = map.length;
    int cols = map[0].length;
    char[][] it = deepCopy(items);
    int pr = -1;
    int pc = -1;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        if (it[i][j] == '@') {
          pr = i;
          pc = j;
        }
      }
    }
    for (int idx = 0; idx < sol.length(); idx++) {
      int dr = 0;
      int dc = 0;
      switch (sol.charAt(idx)) {
        case 'u': dr = -1; break;
        case 'd': dr = 1; break;
        case 'l': dc = -1; break;
        case 'r': dc = 1; break;
        default: return false;
      }
      int ntr = pr + dr;
      int ntc = pc + dc;
      if (ntr < 0 || ntr >= rows || ntc < 0 || ntc >= cols || map[ntr][ntc] == '#') {
        continue;
      }
      if (it[ntr][ntc] == '$') {
        int btr = ntr + dr;
        int btc = ntc + dc;
        if (btr < 0 || btr >= rows || btc < 0 || btc >= cols) {
          continue;
        }
        if (map[btr][btc] == '#' || it[btr][btc] == '$') {
          continue;
        }
        it[btr][btc] = '$';
      }
      it[pr][pc] = ' ';
      it[ntr][ntc] = '@';
      pr = ntr;
      pc = ntc;
    }
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        if (it[i][j] == '$' && map[i][j] != '.') {
          return false;
        }
      }
    }
    return true;
  }

  private static char[][] deepCopy(char[][] g) {
    char[][] out = new char[g.length][];
    for (int i = 0; i < g.length; i++) {
      out[i] = Arrays.copyOf(g[i], g[i].length);
    }
    return out;
  }

  private static String[] listMaps() {
    File dir = new File("maps");
    String[] files = dir.list((d, n) -> n.endsWith(".txt"));
    if (files == null) {
      return new String[0];
    }
    Arrays.sort(files);
    for (int i = 0; i < files.length; i++) {
      files[i] = files[i].substring(0, files[i].length() - 4);
    }
    return files;
  }
}
