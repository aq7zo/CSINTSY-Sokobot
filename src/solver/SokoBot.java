package solver;

public class SokoBot {

  /**
   * Solves a Sokoban puzzle with push-based weighted A* search. See BLUEPRINT.md for the full
   * design. Self-enforces a wall-clock budget below the harness's 15s limit (the harness does not
   * interrupt this thread; it just discards a late result), and never throws to the caller.
   */
  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    long deadline = System.nanoTime() + 14_000L * 1_000_000L;  // 14s, ~1s slack
    try {
      Board board = new Board(width, height, mapData, itemsData);
      if (board.playerStart < 0 || board.crateStart.length == 0) {
        return "";
      }
      String solution = new AStarSolver(board, deadline).solve();
      return solution == null ? "" : solution;
    } catch (Throwable t) {
      // Never hang or crash the harness: a clean return (even empty) beats an exception or a hang.
      return "";
    }
  }

}
