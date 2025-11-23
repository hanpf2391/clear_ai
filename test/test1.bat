@echo off
cd /d C:\
mkdir AIAgentTest_DO_NOT_DELETE
cd AIAgentTest_DO_NOT_DELETE

mkdir temp_files
cd temp_files
echo log > 1.log
echo tmp > 2.tmp
echo bak > 3.bak
cd ..

mkdir project_A
cd project_A
echo pom > pom.xml
mkdir target
cd target
fsutil file createnew largefile.jar 104857600
cd ..\..

mkdir empty_folder

echo ~ > ~backup_document.doc
echo dmp > error_report_20251123.dmp