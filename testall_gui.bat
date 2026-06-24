@echo off
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
