#!/bin/bash
# ---------------------------------------------------------------------------
# GUI batch tester for SokoBot (UNIX counterpart of testall_gui.bat).
#
# Opens the actual game window for every map in maps/ (or just the ones you
# name), one after another, so you can WATCH the bot solve each level.
#
# Uses the program's built-in "check" mode: each map auto-starts the bot
# (no SPACE needed), animates the solution, and then closes by itself and
# advances to the next map. The whole set plays through hands-free.
#
# Usage:
#   ./testall_gui.sh                       play every maps/*.txt
#   ./testall_gui.sh twoboxes1 original1   play only the named levels
#
# (For fast, hands-free PASS/FAIL text output with no GUI, use testall.sh.)
# ---------------------------------------------------------------------------
cd "$(dirname "$0")" || exit 1
find . -name "*.class" -type f -delete
javac src/main/Driver.java -cp src || { echo; echo "Compilation failed."; exit 1; }

if [ "$#" -eq 0 ]; then
  maps=()
  for f in maps/*.txt; do
    maps+=("$(basename "$f" .txt)")
  done
else
  maps=("$@")
fi

for name in "${maps[@]}"; do
  echo
  echo "=== $name ==="
  java -classpath src main.Driver "$name" check
done
echo
echo "Done."
