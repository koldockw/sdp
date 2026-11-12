@echo off
cd /d "%~dp0"
java -jar MoneyPilot.jar
if errorlevel 1 pause
