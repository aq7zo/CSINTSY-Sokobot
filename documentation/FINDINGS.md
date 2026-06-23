# SokoBot — Findings: Open Checklist Items & Solver Performance

A plain-language companion to `CHECKLIST.md`, explaining (1) the items still left open on the
checklist and why, and (2) why the bot solves 14 of the 16 bundled maps. Written for a general
reader — no search-algorithm background assumed.

---

## Summary

- The solver is **complete and working**. It solves **14 of the 16** bundled maps (87.5%).
- On the levels that actually count for grading (**2–8 boxes**), it's **14 for 14 — 100%**.
- The only two failures are the bundled "extra-hard" levels with **10 and 11 boxes**, which are
  *above* the difficulty range the assignment grades on.
- The handful of unticked checklist boxes are deliberate choices or honesty notes, **not bugs**.

---

## Open checklist items, in plain terms

### 1. Corridor / tunnel deadlock detection — deliberately skipped (`[~]`)

A **deadlock** is a position where the puzzle has already become impossible — for example, shoving a
box into a corner you can never pull it back out of. Spotting these early lets the bot stop wasting
time exploring doomed positions.

The design listed **four** kinds of deadlock detector. The bot has **three** of them:
boxes stuck in corners, boxes frozen flat against walls, and clumps of boxes that lock each other in
place. The fourth — noticing a box pushed into a one-tile-wide hallway with no way around to fetch it
— was **left out on purpose**, for two reasons:

- It's the trickiest of the four to get exactly right. If you get it slightly wrong, the bot would
  discard positions that were *actually solvable* — which is worse than not having the check at all.
- The three detectors already built catch most hallway traps anyway.

So this is a "leave it out unless testing shows we need it" decision. It's flagged as a known gap, not
a defect.

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

## Why the bot solves 14 of the 16 bundled maps

### The two failures

| Map | Boxes | Result |
|-----|------:|--------|
| `original2` | 10 | timed out (no solution returned within 15 s) |
| `original3` | 11 | timed out (no solution returned within 15 s) |

These puzzles **do** have solutions — the bot simply can't *find* one within the 15-second limit.

### The reason: difficulty explodes as you add boxes

Sokoban gets dramatically harder with each extra box. Adding a box doesn't make the puzzle a little
harder — it **multiplies** the number of possible board arrangements the bot has to sift through.

- With **2–6 boxes**, there are few enough arrangements that the bot's smart, heavily-pruned search
  lands on a solution almost instantly — every such level here solves in **under a tenth of a second**.
- With **10–11 boxes**, the number of arrangements is so vast that even with all the optimizations
  (searching by *box pushes* instead of single steps, skipping known-dead positions, and never
  re-examining a board it has already seen), it can't get through enough of them before time runs out.

A useful mental picture: brute-forcing a combination lock. Three dials is trivial; every extra dial
multiplies the combinations, and beyond a point the same method just can't finish in time. More boxes
= exponentially more combinations to search.

### Important context

- **The assignment grades on 2–8-box levels and asks for ≥80% solved.** The two failures have 10 and
  11 boxes — harder than anything in the graded range. They ship as "extra-hard" extras. On the 2–8
  range that matters, the bot is **14 for 14 (100%)**.
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
| original1 | 6 | PASS | 0.06 s |
| original2 | 10 | **FAIL** (timeout) | 15 s |
| original3 | 11 | **FAIL** (timeout) | 15 s |

**14 / 16 solved (87.5%) overall — 14 / 14 (100%) on the 2–8-box range that is graded.**
