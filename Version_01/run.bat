@echo off
setlocal
cd /d "%~dp0"

set PORT=%1
if "%PORT%"=="" set PORT=8080

echo ======================================
echo   Starting TA Recruitment System...
echo ======================================

echo.
echo   TA Login:      http://localhost:%PORT%/ta-login.html
echo   MO Login:      http://localhost:%PORT%/mo-login.html
echo   Admin Login:   http://localhost:%PORT%/admin-login.html
echo.
echo ======================================

echo Press Ctrl+C to stop server.
echo.

java -cp "out;lib\gson-2.10.1.jar" com.bupt.tarecruit.Main %*
