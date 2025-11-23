@echo off
chcp 65001 >nul
echo Starting in development mode...
echo.

REM 详细编译输出，便于调试
echo Compiling with detailed output...
call mvn clean compile
if %errorlevel% neq 0 (
    echo.
    echo === COMPILATION ERRORS ===
    echo Fix the errors above and try again.
    pause
    exit /b 1
)

echo.
echo === Starting Application ===
java -Dfile.encoding=UTF-8 -cp "target\classes;target\dependency\*" com.hanpf.clearai.cli.ClaudeTUI

pause