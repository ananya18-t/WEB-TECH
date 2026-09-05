@echo off
echo ===================================================
echo   Compiling Java Servlet Electricity Bill App...
echo ===================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -cp "lib/*;." -d bin src\main\java\com\electricity\*.java WebServer.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo   Starting Embedded Servlet Web Server...
echo ===================================================

java -cp "bin;lib/*;." com.electricity.WebServer
