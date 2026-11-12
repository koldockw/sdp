@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 --release 17 -d out src\moneypilot\*.java test\moneypilot\*.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp out moneypilot.ProjectTest
