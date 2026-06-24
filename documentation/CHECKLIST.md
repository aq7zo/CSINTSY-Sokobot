# SokoBot — Living Implementation Checklist

> **This is the canonical, living checklist** (the snapshot in `BLUEPRINT.md` §0.2 is just a copy).
> **Update this file after every implementation change**: tick what's done, add items you
> discover, and note anything you did *differently* from `BLUEPRINT.md`. Anyone should be able to
> glance here and know what's done / in progress / left without reading the code.
>
> Full design and rationale: see **[`BLUEPRINT.md`](./BLUEPRINT.md)**. Section refs (§) point there.

**Status legend:** `[ ]` not started · `[~]` in progress · `[x]` done · _(note: …)_ deviation.

---

## Must-not-break facts (read before touching anything)

- [x] Coordinate convention respected: **`width = columns`, `height = rows`, arrays `[row][col]`**
      (the signature's param order is misleading — see BLUEPRINT §0.4-A). → `Board`
- [x] Move chars: `u`=row−1, `d`=row+1, `l`=col−1, `r`=col+1 (§0.4-B). → `Board.DR/DC/DCHAR`
- [x] Self-timing implemented — 14s `System.nanoTime()` deadline; harness timer does not kill the thread (§0.4-C, §6).
- [x] Grading pass/fail on a **complete** solve; we never return partial paths (§0.4-C, §6.2).
- [x] **JDK standard library only** — no external jars (§0.4-D).
- [x] `itemsData` crates-on-targets / player-on-target handled (initial state counted correctly) (§0.4-E).
- [x] `original*` ~10-crate levels confirmed stretch/timeout, fail cleanly, not the 2–8 band (§0.4-F).

## Milestones (map onto BLUEPRINT §7 classes)

- [x] **M1 — IO adapter & board model** (§1, §0.4-A) → `Board` _(GameState folded into `Node` — see deviations)_
- [x] **M2 — Static dead-square map** (§5.1) → `DeadlockDetector.computeDeadSquares`
- [x] **M3 — Tunnel macros** (§5.3) → `AStarSolver.tunnelPush` collapses a straight push-run through a
      1-wide corridor into one search edge (move-generation form). Corridor *deadlock* tagging still deferred — see deviations.
- [x] **M4 — State + Zobrist hashing** (§1) → `Board` keys + incremental XOR in `AStarSolver`
- [x] **M5 — Push generator + reachability flood-fill** (§2) → `PathFinder.reachRegion`, `AStarSolver`
- [x] **M6 — Dynamic group-deadlock check** (§5.2) → `DeadlockDetector.isFreezeDeadlock`
- [x] **M7 — Heuristic (matching) + ∞ on deadlock** (§4, §5.4) → `Heuristic` (Hungarian + push-dist maps)
- [x] **M8 — Transposition table + dead marker** (§3.3, §5.4) → `TranspositionTable` over `LongIntMap`
- [x] **M9 — A\* loop (open PQ, closed, tie-break, goal test)** (§3) → `AStarSolver.solve`
- [x] **M10 — Timer + node cap** (§6) → `AStarSolver` (14s deadline, NODE_CAP=3M; first-solution fallback)
- [x] **M11 — Move-string reconstruction** (§2.4) → `AStarSolver.reconstruct` + `PathFinder.appendPath`
- [x] **M12 — Wire into `SokoBot.solveSokobanPuzzle`; JDK-only; try/catch→`""`** (§6.3, §7)
- [x] **M13 — Test harness** (§9): `solver/Harness.java` runs all `maps/`, simulates + verifies solutions

## Per-milestone isolation test done?

Verified end-to-end via `Harness` (simulate-and-verify) rather than per-class unit tests:
- [x] M11 output re-simulates (GamePanel rules) to all-crates-on-targets for every solved level
- [x] M9 solves `twoboxes*`, `threeboxes*`, `fourboxes*`, `fiveboxes*` in <0.1s each
- [ ] (Optional) add JUnit-style isolation tests per class if the team wants finer coverage

## Integration / acceptance (verified 2026-06-21)

- [x] Compiles via clean `sokobot.bat` path (`del *.class` + `javac src/main/Driver.java -cp src`) — all 9 solver classes produced, exit 0.
- [ ] `sokobot testlevel` solves and animates (GUI not run headlessly here; logic verified via Harness).
- [x] T1 (1–2 crates) all pass — testlevel, twoboxes1/2/3.
- [x] T2 (3–5 crates) all pass within 15s — threeboxes*, fourboxes*, fiveboxes*.
- [x] T3 / stretch: original1 (6 crates) PASS; original2 (10 crates) now PASS ~0.7s after tunnel macros;
      original3 (11 crates) still times out cleanly (return "", no crash). Both originals exceed the 2–8 grading band.
- [x] Never hangs or throws to the harness (returns `""` on give-up / any exception).
- [x] **Bundled-set result: 15/16 PASS (93.8%); 14/14 PASS on the 2–8 crate band.** Comfortably ≥80% target.
      (Only failure: original3/11 crates — above the band.) Run with `testall.bat`.
- [x] Tuning recorded: `w`=1.5 (W_NUM/W_DEN=3/2), budget=14.0s, `NODE_CAP`=3,000,000, tunnel macros on.

## Deviations from BLUEPRINT (keep this honest)

- **GameState folded into `Node`** (§7 listed a separate `GameState`). Reason: per-node memory — millions of
  nodes; avoiding a second object per node matters under the budget. `Node` carries crates/playerNorm/hash directly.
- **Tunnel handling is a move-macro, not a deadlock check (§5.3).** `tunnelPush` collapses a straight
  push-run through a 1-wide corridor into one edge. Sound: only interior tunnel cells are skipped; a crate
  is never forced into a junction (where stopping can be necessary — the bug that briefly broke fourboxes1
  before the "stop at the last tunnel cell" fix). This is what cracked original2. Corridor *deadlock*
  detection proper is still not implemented (soundness risk for low marginal value; static dead squares +
  freeze cover the common traps). Counts as a known gap.
- **Anytime improvement (§6.2 step 2–3) not implemented.** We return the FIRST complete solution found
  (greedy-leaning weighted A*). Reason: passes grading; the improvement phase only shortens move counts.
  Move counts are longer-than-optimal (e.g., original1 = 437 moves) — acceptable for pass/fail.

## Open decisions log (resolved for the initial build — revisit on the hidden set)

- [x] Heuristic weight `w`: **1.5** — solves the whole 2–8 band in <0.1s. (Empirically, raising `w` up to
      ~30 does NOT help the remaining hard level — see the original3 note below.)
- [x] Time budget slack: **14.0s** (1s under the 15s limit) — no late returns observed.
- [x] `NODE_CAP`: **3,000,000** — time limit bites first on the hard levels; lower if a grader OOMs on a small heap.
- [x] First-goal vs. anytime: **first-goal** (simplest; passes). Revisit only if move-count quality matters.
- [x] Hungarian vs. greedy matching: **Hungarian** (exact, n≤~12, negligible cost).
- [x] **original3 (11 crates) left unsolved — diagnosed, not a tuning gap.** With a 120s budget + 100M-node
      cap it still fails after exploring ~12M states, so it is *guidance/pruning*-bound, not throughput-bound:
      the matching heuristic is blind to the goal-room packing order, and there is no corral/packing deadlock
      check. Cracking it needs corral (PI-corral) pruning or a packing-aware heuristic — substantial work with
      real regression risk — for a map above the graded band. Deliberately not pursued.

## Change log (newest first)

- 2026-06-24 — Implemented **tunnel macros** (`AStarSolver.tunnelPush`/`isTunnel`; `Node.pushCount`):
  a straight push-run through a 1-wide corridor collapses into one search edge, and reconstruction emits
  the whole run. Sound — only interior tunnel cells are skipped; a crate is never forced into a junction
  (the bug that briefly broke fourboxes1 before the "stop at the last tunnel cell" fix). Result:
  **15/16 (was 14/16)** — original2 (10 crates) now solves in ~0.7s; all 14 graded-band maps still pass,
  no regressions. Weight and transposition-table-size were swept empirically and found to be non-levers,
  so both stay at baseline. original3 (11 crates) remains unsolved and is diagnosed as guidance-bound
  (see open-decisions note), not throughput-bound.
- 2026-06-21 — Added `FINDINGS.md` (layman write-up of open checklist items + why 14/16 solve) and
  `testall_gui.bat`/`testall_gui.sh` (opens the game window per map to watch the bot solve; the
  existing `testall.bat`/`.sh` remain the fast headless PASS/FAIL runner). Removed two stale duplicate
  lines from the open-decisions log.
- 2026-06-21 — Initial full implementation of the push-based weighted-A\* solver per BLUEPRINT.
  Classes: `LongIntMap`, `Board`, `Node`, `TranspositionTable`, `DeadlockDetector`, `Heuristic`,
  `PathFinder`, `AStarSolver`, `SokoBot` (+ dev-only `Harness`). Verified: 14/16 bundled maps pass
  (14/14 on the 2–8 crate band, all <0.1s); original2/3 (10–11 crates) time out cleanly. Added
  `testall.bat`/`testall.sh` headless all-maps runner. Deviations:
  GameState folded into Node, corridor deadlock (§5.3) deferred, first-solution (no anytime refine).
- _YYYY-MM-DD — (name) — what changed / any deviation from BLUEPRINT._
