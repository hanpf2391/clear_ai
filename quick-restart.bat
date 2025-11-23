@echo off
chcp 65001 >nul
echo Quick restart with latest code...
echo.

REM 快速编译并运行
call mvn compile -q && java -Dfile.encoding=UTF-8 -cp "target\classes;target\dependency\*" com.hanpf.clearai.cli.ClaudeTUI

pause