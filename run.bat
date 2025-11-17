@echo off
echo Starting JavaFX Chat Application...

rem 设置JavaFX路径 (请根据实际情况修改)
set JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-17.0.2\lib

rem 设置Java类路径
set CLASS_PATH=target\classes

rem 启动应用
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp "%CLASS_PATH%" com.hanpf.clearai.JavaFxChatDemo

pause