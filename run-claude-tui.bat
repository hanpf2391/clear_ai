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

REM 每次都重新编译以确保使用最新代码
echo Compiling project with latest changes...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo Compilation failed
    pause
    exit /b 1
)

REM 如果依赖不存在，复制依赖
if not exist "target\dependency" (
    echo Copying dependencies...
    call mvn dependency:copy-dependencies -q
    if %errorlevel% neq 0 (
        echo Failed to copy dependencies
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