@echo off
cd /d "%~dp0"
del /s /q *.class >nul 2>&1
javac -cp src src\main\Driver.java src\solver\Harness.java
if errorlevel 1 (
  echo.
  echo Compilation failed.
  exit /b 1
)
java -cp src solver.Harness %*
