#!/bin/bash
cd "$(dirname "$0")" || exit 1
find . -name "*.class" -type f -delete
javac -cp src src/main/Driver.java src/solver/Harness.java || { echo; echo "Compilation failed."; exit 1; }
java -cp src solver.Harness "$@"
