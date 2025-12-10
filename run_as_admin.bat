@echo off
echo ==========================================
echo ClearAI Admin Mode - Full Access Scanner
echo ==========================================
echo.
echo This will run the program with admin privileges
echo to scan ALL files including system protected files.
echo.
echo Press Ctrl+C to cancel, or any key to continue...
pause > nul

echo.
echo Requesting administrator privileges...
echo.

REM Check if already running as admin
net session >nul 2>&1
if %errorLevel% == 0 (
    echo Running with admin privileges...
) else (
    echo Requesting elevation...
    powershell -Command "Start-Process cmd -ArgumentList '/c cd /d \"%~dp0\" && java -cp \"classes\" com.hanpf.clearai.clustering.SimpleClusteringDemo' -Verb RunAs"
    exit /b
)

cd /d "%~dp0"
echo Current directory: %CD%
echo.

echo Starting ClearAI with full admin privileges...
java -cp "classes" com.hanpf.clearai.clustering.SimpleClusteringDemo

echo.
echo Program finished. Press any key to exit...
pause > nul