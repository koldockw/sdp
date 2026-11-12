@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 --release 17 -d out src\moneypilot\*.java
if errorlevel 1 exit /b 1
jar --create --file MoneyPilot.jar --main-class moneypilot.Main -C out .
