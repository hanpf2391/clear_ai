@echo off
chcp 65001 >nul
title 🧹 CLEAR AI 测试靶场清理器
color 0E

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                  🧹 CLEAR AI 测试靶场清理器                      ║
echo ║                                                             ║
echo ║  此脚本将清理所有测试文件，恢复系统到测试前状态                   ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

:: 检查测试目录是否存在
if not exist "C:\AIAgentTest_DO_NOT_DELETE" (
    echo ℹ️  测试目录不存在，无需清理
    pause
    exit /b 0
)

echo ⚠️  警告: 即将删除测试目录及其所有内容
echo    目录路径: C:\AIAgentTest_DO_NOT_DELETE
echo.

:: 显示目录内容让用户确认
echo 📋 测试目录内容预览:
echo ----------------------------------------
dir "C:\AIAgentTest_DO_NOT_DELETE" /s /b 2>nul
if %errorLevel% neq 0 (
    echo   (无法访问某些文件，可能需要管理员权限)
)
echo ----------------------------------------
echo.

:: 询问用户确认
set /p confirm="确定要删除测试目录吗? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo ❌ 用户取消操作
    pause
    exit /b 0
)

echo.
echo 🧹 正在清理测试环境...

:: 尝试正常删除
echo [1/3] 尝试删除测试目录...
rmdir /s /q "C:\AIAgentTest_DO_NOT_DELETE" 2>nul

:: 检查是否删除成功
if exist "C:\AIAgentTest_DO_NOT_DELETE" (
    echo ⚠️  正常删除失败，尝试强制删除...

    :: 强制删除 (需要管理员权限)
    echo [2/3] 尝试强制删除...
    takeown /f "C:\AIAgentTest_DO_NOT_DELETE" /r /d y 2>nul
    icacls "C:\AIAgentTest_DO_NOT_DELETE" /grant administrators:F /t 2>nul
    rmdir /s /q "C:\AIAgentTest_DO_NOT_DELETE" 2>nul
)

:: 最终检查
if exist "C:\AIAgentTest_DO_NOT_DELETE" (
    echo [3/3] 删除仍然失败，尝试手动删除关键文件...

    :: 手动删除一些关键文件
    if exist "C:\AIAgentTest_DO_NOT_DELETE\temp_files" (
        del /f /q "C:\AIAgentTest_DO_NOT_DELETE\temp_files\*" 2>nul
    )
    if exist "C:\AIAgentTest_DO_NOT_DELETE\secret" (
        attrib -h -r "C:\AIAgentTest_DO_NOT_DELETE\secret" 2>nul
        rmdir /s /q "C:\AIAgentTest_DO_NOT_DELETE\secret" 2>nul
    )

    :: 最后尝试
    rmdir /s /q "C:\AIAgentTest_DO_NOT_DELETE" 2>nul
)

:: 最终结果检查
if exist "C:\AIAgentTest_DO_NOT_DELETE" (
    echo.
    echo ❌ 清理失败: 测试目录仍然存在
    echo 💡 建议: 以管理员权限运行此脚本，或手动删除
    echo    路径: C:\AIAgentTest_DO_NOT_DELETE
) else (
    echo.
    echo ✅ 清理成功! 测试环境已完全移除
)

echo.
echo 📊 清理报告:
echo   清理目标: C:\AIAgentTest_DO_NOT_DELETE
echo   执行时间: %date% %time%
echo.

pause