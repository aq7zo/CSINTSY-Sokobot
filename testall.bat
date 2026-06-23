@echo off
REM ---------------------------------------------------------------------------
REM Headless batch tester for SokoBot.
REM
REM Compiles the solver (+ the dev-only Harness) and runs the bot on every map in
REM maps\, then prints PASS/FAIL, solve time, and move count per level plus a
REM summary line. Each returned solution is re-simulated with the game's own
REM movement rules to confirm it really solves the level. No GUI, no SPACE press.
REM
REM Usage:
REM   testall                         run every maps\*.txt
REM   testall twoboxes1 fiveboxes3    run only the named levels
REM ---------------------------------------------------------------------------
cd /d "%~dp0"
del /s /q *.class >nul 2>&1
javac -cp src src\main\Driver.java src\solver\Harness.java
if errorlevel 1 (
  echo.
  echo Compilation failed.
  exit /b 1
)
java -cp src solver.Harness %*
