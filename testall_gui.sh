#!/bin/bash
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
