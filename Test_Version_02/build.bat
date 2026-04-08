@echo off
echo ========================================
echo  Building TA Recruitment System v2
echo ========================================

if not exist lib\gson-2.10.1.jar (
    echo Downloading Gson library...
    mkdir lib 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'lib\gson-2.10.1.jar'"
    if not exist lib\gson-2.10.1.jar (
        echo ERROR: Failed to download Gson. Please place gson-2.10.1.jar in the lib folder manually.
        pause
        exit /b 1
    )
)

if not exist out mkdir out

echo Compiling Java sources...
javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar" -d out src\main\java\com\bupt\tarecruit\model\*.java src\main\java\com\bupt\tarecruit\service\*.java src\main\java\com\bupt\tarecruit\handler\*.java src\main\java\com\bupt\tarecruit\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo Build FAILED!
    pause
    exit /b 1
)

echo Build SUCCESS!
echo Run with: run.bat
pause
