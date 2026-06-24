# SokoBot — Findings: Open Checklist Items & Solver Performance

A plain-language companion to `CHECKLIST.md`, explaining (1) the items still left open on the
checklist and why, and (2) why the bot solves 15 of the 16 bundled maps. Written for a general
reader — no search-algorithm background assumed.

---

## Summary

- The solver is **complete and working**. It solves **15 of the 16** bundled maps (93.8%).
- On the levels that actually count for grading (**2–8 boxes**), it's **14 for 14 — 100%**.
- The only failure is the bundled "extra-hard" level with **11 boxes** (`original3`), which is
  *above* the difficulty range the assignment grades on. (`original2`, 10 boxes, now solves too,
  thanks to tunnel macros.)
- The handful of unticked checklist boxes are deliberate choices or honesty notes, **not bugs**.

---

## Open checklist items, in plain terms

### 1. Corridor / tunnel handling — tunnel *macros* added; corridor *deadlock* check still skipped

Two different things share the word "tunnel" here, so keep them separate:

**(a) Tunnel macros — now implemented.** When a box is shoved straight into a one-tile-wide corridor,
the only sensible thing is to keep pushing it the same way (the pusher ends up right behind it after
each shove, so there's nothing else it can do). The bot now treats that whole straight run as a
*single* move instead of one move per tile. This shrinks the search a lot on maps with corridors, and
it is what finally let the bot solve `original2` (10 boxes). It is carefully written to be **safe**: it
only skips the *middle* of a corridor, and never forces a box out the far end into an open junction
(where stopping really can matter) — getting that wrong briefly broke a 4-box level during development
before it was fixed.

**(b) Corridor *deadlock* detection — still skipped.** This is the separate idea of *recognising* that
a box shoved into a dead-end hallway is already lost. The bot has three of the four planned deadlock
detectors (boxes stuck in corners, boxes frozen against walls, and clumps that lock each other in
place); this fourth one is **left out on purpose** because it is the easiest to get subtly wrong, and a
wrong deadlock check throws away *solvable* positions — worse than not having it. Flagged as a known
gap, not a defect.

### 2. Optional per-component unit tests — not written (`[ ]`)

The plan suggested writing small tests for each internal piece in isolation (test the box-to-target
matching math on its own, test the position-hashing on its own, and so on).

Instead, the bot was tested the strongest practical way: **run it on every map and confirm the moves
it produces actually solve the puzzle** (the `testall.bat` runner does exactly this). The per-piece
tests are a "nice to have" for finer-grained confidence — useful for the team later, but not required
for the solver to work correctly.

### 3. Watching the on-screen animation — not done in automation (`[ ]`)

The bot's "brain" was verified by running it **without** the graphical window (faster, and it can be
automated), then checking that its list of moves genuinely solves each level. The actual game window —
where you watch the little robot push boxes around — was **not** opened during automated testing,
because launching a GUI window from an automated environment is unreliable.

The moves are proven correct, so when **you** run `sokobot testlevel` (or the new `testall_gui.bat`)
the animation will play and solve the level. You can tick this box yourself once you've watched it run.

---

## Why the bot solves 15 of the 16 bundled maps

### The one remaining failure

| Map | Boxes | Result |
|-----|------:|--------|
| `original3` | 11 | timed out (no solution returned within 15 s) |

This puzzle **does** have a solution — the bot simply can't *find* one within the 15-second limit.
`original2` (10 boxes) used to fail here too, but the new tunnel macros now solve it in well under a second.

### The reason: it's lost, not just slow

Sokoban gets dramatically harder with each extra box: each one **multiplies** the number of board
arrangements to sift through. With **2–6 boxes** the bot's heavily-pruned search lands on a solution
almost instantly (every such level here is under a tenth of a second).

`original3` is harder than that for a *specific* reason, and we checked it directly: when given **120
seconds** (instead of 15) it *still* failed, after looking at about **12 million** positions. So it is
**not simply "too slow"** — more time doesn't help. The trouble is that all 11 targets sit in one
cramped room with a single doorway, and the boxes must be packed in exactly the right order. The bot's
"sense of direction" (the estimate it uses to decide which move looks promising) doesn't understand
packing order, so it keeps wandering into arrangements that look fine but can't actually be finished.

Solving puzzles like this needs a smarter dead-end detector (recognising a doomed *room*, not just a
doomed box) or a packing-order-aware estimate — both are big, delicate additions that could easily
break the 15 maps that already work, so for an out-of-grading-range level they were not pursued.

### Important context

- **The assignment grades on 2–8-box levels and asks for ≥80% solved.** The one failure has 11 boxes —
  harder than anything in the graded range. It ships as an "extra-hard" extra. On the 2–8 range that
  matters, the bot is **14 for 14 (100%)**.
- **When it can't solve in time, it gives up cleanly** — no crash, no hang; it simply reports a
  timeout. That is the correct behavior.

---

## Results at a glance

Full run via `testall.bat` (all numbers from the verified headless run):

| Map | Boxes | Result | Time |
|-----|------:|--------|-----:|
| testlevel | 4 | PASS | 0.00 s |
| twoboxes1 / 2 / 3 | 2 | PASS | 0.00 s |
| threeboxes1 / 2 / 3 | 3 | PASS | ≤0.01 s |
| fourboxes1 / 2 / 3 | 4 | PASS | ≤0.04 s |
| fiveboxes1 / 2 / 3 | 5 | PASS | ≤0.08 s |
| original1 | 6 | PASS | ≤0.05 s |
| original2 | 10 | **PASS** | ~0.7 s |
| original3 | 11 | **FAIL** (timeout) | 15 s |

**15 / 16 solved (93.8%) overall — 14 / 14 (100%) on the 2–8-box range that is graded.**
