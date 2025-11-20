@echo off
chcp 65001 >nul
echo Starting Claude Code TUI Demo...
echo.

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java not found, please install Java 17 or higher
    pause
    exit /b 1
)

REM 检查编译状态
if not exist "target\classes\com\hanpf\clearai\cli\ClaudeTUI.class" (
    echo Compiling project...
    call mvn clean compile -q
    if %errorlevel% neq 0 (
        echo Compilation failed
        pause
        exit /b 1
    )
)

REM 设置类路径并运行
echo Starting...
set CLASSPATH=target\classes;target\dependency\*
java -Dfile.encoding=UTF-8 -cp "%CLASSPATH%" com.hanpf.clearai.cli.ClaudeTUI

if %errorlevel% neq 0 (
    echo.
    echo Startup failed
    pause
)

echo.
echo Program exited
pause