package solver;

public class SokoBot {

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    long deadline = System.nanoTime() + 14_000L * 1_000_000L;
    try {
      Board board = new Board(width, height, mapData, itemsData);
      if (board.playerStart < 0 || board.crateStart.length == 0) {
        return "";
      }
      String solution = new AStarSolver(board, deadline).solve();
      return solution == null ? "" : solution;
    } catch (Throwable t) {
      return "";
    }
  }

}
