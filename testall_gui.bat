@echo off
REM ---------------------------------------------------------------------------
REM GUI batch tester for SokoBot.
REM
REM Opens the actual game window for every map in maps\ (or just the ones you
REM name), one after another, so you can WATCH the bot solve each level.
REM
REM Uses the program's built-in "check" mode: each map auto-starts the bot
REM (no SPACE needed), animates the solution, and then closes by itself and
REM advances to the next map. The whole set plays through hands-free.
REM
REM Usage:
REM   testall_gui                      play every maps\*.txt
REM   testall_gui twoboxes1 original1  play only the named levels
REM
REM (For fast, hands-free PASS/FAIL text output with no GUI, use testall.bat.)
REM ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
del /s /q *.class >nul 2>&1
javac src\main\Driver.java -cp src
if errorlevel 1 (
  echo.
  echo Compilation failed.
  pause
  exit /b 1
)

if "%~1"=="" (
  for %%F in (maps\*.txt) do call :run "%%~nF"
) else (
  for %%A in (%*) do call :run "%%~A"
)
echo.
echo Done.
endlocal
exit /b 0

:run
echo.
echo === %~1 ===
java -classpath src main.Driver "%~1" check
goto :eof
