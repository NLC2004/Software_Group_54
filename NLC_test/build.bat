@echo off
echo ========================================
echo   Building TA Recruitment System...
echo ========================================

if not exist lib\gson-2.10.1.jar (
    echo Downloading Gson library...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'lib\gson-2.10.1.jar'"
)

if exist out rmdir /s /q out
mkdir out

echo Compiling Java sources...
javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar" -d out ^
    src\main\java\com\bupt\tarecruit\model\User.java ^
    src\main\java\com\bupt\tarecruit\model\Job.java ^
    src\main\java\com\bupt\tarecruit\model\Application.java ^
    src\main\java\com\bupt\tarecruit\service\DataService.java ^
    src\main\java\com\bupt\tarecruit\handler\BaseHandler.java ^
    src\main\java\com\bupt\tarecruit\handler\StaticHandler.java ^
    src\main\java\com\bupt\tarecruit\handler\AuthHandler.java ^
    src\main\java\com\bupt\tarecruit\handler\JobHandler.java ^
    src\main\java\com\bupt\tarecruit\handler\ApplicationHandler.java ^
    src\main\java\com\bupt\tarecruit\handler\AdminHandler.java ^
    src\main\java\com\bupt\tarecruit\handler\UploadHandler.java ^
    src\main\java\com\bupt\tarecruit\Main.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo BUILD FAILED
    pause
    exit /b 1
)

echo.
echo BUILD SUCCESSFUL
echo Run 'run.bat' to start the server.
