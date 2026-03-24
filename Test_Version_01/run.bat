@echo off
cd /d "%~dp0"
java -cp "out;lib\gson-2.10.1.jar" com.bupt.tarecruit.Main %*
