@echo off
cd /d C:\AIAgentTest_DO_NOT_DELETE

fsutil file createnew "大型安装包.exe" 209715200

:: 创建ZIP包需要手动操作或使用 PowerShell
powershell -Command "Compress-Archive -Path '.\temp_files\*' -DestinationPath '项目归档_2022.zip'"

echo xlsx > "公司财务报表.xlsx"
echo jpg > old_photo.jpg
powershell -Command "Set-ItemProperty -Path 'old_photo.jpg' -Name LastWriteTime -Value '2020-01-01'"