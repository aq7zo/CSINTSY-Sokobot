# SokoBot Implementation Report
**CSINTSY - Sokoban Solver Project**

---

## 1. SokoBot Algorithm

The SokoBot solver is implemented using a high-throughput, memory-optimized **Weighted A* Search** algorithm. It is structured around several custom sub-systems designed to minimize state space expansion, optimize memory footprint, and provide high-accuracy heuristic guidance.

### A. State Representation
In high-state search tasks like Sokoban, memory allocation overhead is a frequent bottleneck. To address this, the solver merges the canonical board state directly into the `Node` class, eliminating the overhead of keeping separate state and node objects.
* **Crate Representation:** The coordinates of all active crates are stored as a sorted, primitive `int[]` array. Sorting ensures consistent array comparisons and hashing.
* **Player Normalization:** Instead of tracking the exact coordinate of the player, the solver normalizes the player position to the **minimum cell ID reachable** by the player in the current configuration. This player-normalization technique ensures that two states with identical box placements and player locations in the same open region are treated as a single unique state, preventing redundant path exploration.
* **Incremental Hashing:** The state is indexed using a 64-bit Zobrist hash key. The key is updated incrementally during search transitions using bitwise XOR operations, avoiding expensive full-board scans:
  $$\text{hash}_{\text{new}} = \text{hash}_{\text{old}} \oplus zPlayer[player_{\text{old}}] \oplus zPlayer[player_{\text{new}}] \oplus zCrate[crate_{\text{old}}] \oplus zCrate[crate_{\text{new}}]$$

### B. Action Generation (Branching on Pushes)
Branching on individual player steps (up, down, left, right) produces a large depth-to-solution ratio. The solver drastically reduces the branching factor by branching **exclusively on legal box pushes**:
1. At each node, a flood-fill Breadth-First Search (BFS) is executed from the player's position to identify all reachable tiles.
2. For each crate, the solver checks if the player can access the tile directly opposite a target push direction.
3. If the opposite tile is reachable, and the destination cell is not a wall, another box, or a precalculated dead square, the push is marked as a legal transition.

### C. Search Strategy & Heuristic Estimation
* **Weighted A* Search:** The evaluation function is configured as $f(n) = g(n) + W \cdot h(n)$, using a weight $W = 1.5$ (represented as a fraction $3/2$ in integer arithmetic to avoid floating-point overhead). This weight leans greedily towards promising goal paths, significantly increasing search speed in exchange for minor compromises in path length optimality.
* **Hungarian Algorithm Matching Heuristic:** The heuristic $h(n)$ is a wall-aware distance lower bound. In a preprocessing step, a reverse BFS computes the exact push distance from each target to all tiles on the board, assuming a single crate and ignoring other boxes. During search, the heuristic cost of a state is calculated as the **Minimum-Cost Perfect Matching** of crates to targets using the Hungarian Algorithm ($O(k^3)$ complexity, where $k$ is the number of boxes). Unlike simple Manhattan distance, Hungarian matching guarantees that no two crates are matched to the same target, yielding a tight, admissible lower bound.
* **Transposition Table:** Visited states are deduped using an open-addressed `LongIntMap` built with primitive `long[]` keys and `int[]` values. This avoids standard Java `HashMap` boxing overhead, allowing millions of states to be stored in memory under heap limits.

Weighted A* was chosen over BFS, plain A*, and IDA* because the project goal prioritizes solving as many maps as possible within the 15-second limit rather than proving shortest-path optimality. BFS is impractical because it expands by shallow depth without heuristic guidance, while plain A* spends more time preserving optimality guarantees that are not required by the grading criteria. IDA* reduces memory usage, but repeated depth-bound searches can waste time on Sokoban maps with many repeated crate configurations. Weighted A* fits the pass/fail objective better: it keeps the useful structure of A* while biasing the search toward states that appear closer to solved, which improves the chance of producing a complete valid solution before timeout.

### D. Deadlock Pruning and Tunnel Macros
* **Static Dead Squares:** Computed once at startup via a reverse BFS "pull" from all target cells. Floor squares from which a crate can never be pulled to any target (e.g., corners, or lines along walls without targets) are marked static dead. Nodes where a box lands on a static dead square are immediately pruned.
* **Dynamic Freeze Deadlocks:** A recursive check determines if the pushed box, or any of its box neighbors, has been pinned against walls, corners, or other already-frozen boxes such that it can no longer move along either axis. If a box is frozen and is not occupying a goal, the state is pruned. A frozen-box partition check further verifies that frozen boxes do not cut off empty goal cells from the player's reachable region.
* **Tunnel Macros:** When a box is pushed into a 1-wide corridor (walled on both sides), the solver collapses the consecutive push actions along the tunnel into a single macro move. The macro extends the push until the box reaches a junction, a target, an obstacle, or a static dead square. This reduces the search depth and saves millions of node allocations on maps with corridor layouts.
* **Deferred Corridor Deadlock Detection:** More aggressive corridor deadlock rules were considered, but were deliberately deferred because an unsound detector could incorrectly prune solvable states. The final solver favors conservative pruning that preserves correctness.

### E. A* Player Pathfinding (Move Reconstruction)
The player's walk between box pushes is computed during solution reconstruction using an **A\* search** with Manhattan distance as an admissible heuristic, replacing the original plain BFS:
* **A\*** prioritizes walking toward the push destination by maintaining a min-heap ordered by $f(n) = g(n) + h(n)$, where $h(n)$ is the Manhattan distance to the target cell.
* On boards with open layouts, this visits fewer cells than BFS (which expands uniformly in all directions), producing faster reconstruction without changing solution correctness.
* The heuristic is admissible (Manhattan distance never overestimates on a grid), so A\* always returns the shortest path.

---

## 2. Evaluation and Performance

The solver was evaluated using a headless test harness running 19 test maps on a Windows OS environment. 

### Performance Summary
* **Graded Range (2-8 boxes):** 16/16 solved (100% success rate, all in under 0.14s).
* **Extended Range (9-11 boxes):** 2/3 solved (`nineboxes1` solved instantly; `original2` solved in 1.92s).
* **Overall Metrics:** 18/19 maps solved (94.7% success rate), total search time = 16.30s.

| Map Name | Boxes | Result | Execution Time | Solution Length | Performance Category |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `testlevel` | 4 | PASS | 0.00s | 37 | Highly Optimized |
| `twoboxes1 / 2 / 3` | 2 | PASS | 0.00s | 38 / 48 / 50 | Highly Optimized |
| `threeboxes1 / 2 / 3` | 3 | PASS | 0.00s | 91 / 151 / 85 | Highly Optimized |
| `fourboxes1 / 2 / 3` | 4 | PASS | 0.00s-0.02s | 95 / 182 / 221 | Highly Optimized |
| `fiveboxes1 / 2 / 3` | 5 | PASS | 0.04s-0.14s | 96 / 173 / 262 | Highly Optimized |
| `original1` | 6 | PASS | 0.04s | 437 | Highly Optimized |
| `sevenboxes1` | 7 | PASS | 0.01s | 22 | Highly Optimized |
| `eightboxes1` | 8 | PASS | 0.03s | 25 | Highly Optimized |
| `nineboxes1` | 9 | PASS | 0.00s | 29 | Highly Optimized |
| `original2` | 10 | PASS | 1.92s | 648 | Solved via Tunnel Macros |
| `original3` | 11 | **FAIL** | 14.02s (Timeout) | N/A | Heuristic Blindness |

### Analysis of Strengths
* **Hallways and Long Corridors:** Maps containing narrow tunnels (e.g., `original2`) are solved quickly due to **Tunnel Macros**. By packing straight corridor pushes into single edges, the search graph is compressed, resulting in quick solves for deep search horizons.
* **Low-to-Medium Box Counts:** For puzzles under 7 boxes, the combination of static dead-square pruning, freeze deadlock checks, and Hungarian matching computes a search space small enough to resolve within fractions of a second.
* **Impact of Tunnel Macros:** Before tunnel macros were added, `original2` timed out. After the macro transition logic was implemented, the same 10-box map became solvable in 1.92s with a 648-move solution. This shows that the biggest improvement came from reducing push depth in corridor-heavy boards, not only from changing the heuristic.

### Analysis of Weaknesses
* **Interdependent Packing Constraints:** The solver fails on `original3` (11 boxes). This puzzle requires boxes to be packed into a narrow goal room with a single entrance in a precise order. Because the Hungarian algorithm assumes boxes can move independently, it does not model the physical blockage that crates cause each other (congestion).
* As a result, the solver pushes boxes toward the goals in a way that minimizes independent distances, creating a block at the entrance room. The solver is trapped in a region of low heuristic values that are actually unsolvable (guidance-bound failure), timing out after 14 seconds.

---

## 3. Challenges and Reflections

### Development Challenges
1. **Balancing Speed and Correctness:** The team had to tune pruning rules carefully. Dead-square and freeze checks improved speed, but each new pruning rule had to be kept sound so that the solver would not accidentally discard valid solutions.
2. **Tunnel Macro Edge Cases:** Implementing tunnel macros required special care around corridor exits, junctions, targets, and blocked cells. Early versions could over-extend a macro or stop at the wrong place, so the logic had to be adjusted until corridor pushes still represented legal Sokoban moves.
3. **Memory Pressure in Java:** The solver creates and compares many states, so standard object-heavy structures created too much overhead. This led to the use of sorted primitive arrays, incremental hashing, and a custom primitive transposition table.
4. **`original3` Remaining Failure:** The hardest unresolved issue is not raw movement search, but packing order. `original3` needs boxes to enter a constrained goal room in a correct sequence, while the current heuristic mainly sees independent distances to goals. Future work would focus on packing-order heuristics, corral deadlock detection, and possibly an anytime search mode that first finds a solution quickly and then improves it if time remains.

### General Sokoban Solver Challenges
1. **State Space Explosion:** Sokoban is PSPACE-complete. Adding a single box multiplies the possible arrangements of the board, causing search graphs to grow exponentially. Solvers must rely on aggressive pruning to remain within memory and time limits.
2. **The Soundness-Pruning Tradeoff:** Implementing aggressive deadlock checks (e.g., identifying when a room is blocked or a hallway is closed) carries a high risk of being *unsound*. If a deadlock check contains a minor logic error and flags a solvable state as a deadlock, the solver will prune that node, resulting in a failure to find the solution. Keeping deadlock detection mathematically sound limits how aggressively the search space can be pruned.
3. **Heuristic Congestion Blindness:** Traditional heuristics (Manhattan distance, perfect matching) calculate distances by assuming a clean, empty board for each crate. They do not account for the fact that boxes act as walls to other boxes. In tight rooms or narrow goal zones, this lack of congestion awareness leads the search to repeatedly explore dead-end packing orders.
4. **JVM Memory Allocation Overhead:** Instantiating millions of objects (such as nodes, states, or hash maps) in Java triggers heavy garbage collection overhead, which eats into the execution time budget. Utilizing flat, primitive arrays and open-addressed maps is necessary to bypass JVM object-creation bottlenecks.

---

## 4. Table of Contributions

| Member | Main Contributions | Associated Milestones & Commits |
| :--- | :--- | :--- |
| **Enzo Campo** | Designed the initial Weighted A* search framework, Hungarian matching heuristic, static dead square detector, transposition table, and freeze group deadlock checkers. | Commit `6a1d0ae` (Version 1.0) |
| **Jack** | Refactored board state representation into memory-efficient node properties, implemented Tunnel Macros in AStarSolver, corrected corridor junction bugs, and enabled the solving of `original2`. | Commits `eb27cf3`, `3086b70`, `8208d62` (Version 2.0) |
| **Jade Yabut** | Added frozen-box partition deadlock check to `DeadlockDetector`, upgraded player path reconstruction from BFS to A\* with Manhattan heuristic in `PathFinder`, conducted integration testing via the `Harness` utility, carried out GUI verification runs, tracked bug regressions, and compiled the final implementation report. | Version 3.0 (Partition Check + A\* Pathfinding), Validation, Report Prep |
