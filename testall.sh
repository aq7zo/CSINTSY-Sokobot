#!/bin/bash
# ---------------------------------------------------------------------------
# Headless batch tester for SokoBot (UNIX counterpart of testall.bat).
#
# Compiles the solver (+ the dev-only Harness) and runs the bot on every map in
# maps/, then prints PASS/FAIL, solve time, and move count per level plus a
# summary. Each returned solution is re-simulated with the game's own movement
# rules to confirm it really solves the level. No GUI, no SPACE press.
#
# Usage:
#   ./testall.sh                       run every maps/*.txt
#   ./testall.sh twoboxes1 fiveboxes3  run only the named levels
# ---------------------------------------------------------------------------
cd "$(dirname "$0")" || exit 1
find . -name "*.class" -type f -delete
javac -cp src src/main/Driver.java src/solver/Harness.java || { echo; echo "Compilation failed."; exit 1; }
java -cp src solver.Harness "$@"
