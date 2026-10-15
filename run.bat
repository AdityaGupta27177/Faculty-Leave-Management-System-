@echo off
title Faculty Leave Management System

echo ====================================================================
echo        FACULTY LEAVE MANAGEMENT SYSTEM
echo        Semester III Industry Practice-I Project
echo ====================================================================
echo.

set APP_DIR=%~dp0
cd /d "%APP_DIR%"

if "%PORT%"=="" set PORT=8080

echo [1/4] Environment Config:
echo       PORT: %PORT%
echo       DB: Managed via db.properties (Supabase / PostgreSQL)
echo.

if not exist "backend\lib" mkdir "backend\lib"

echo [2/4] Checking dependencies...
if not exist "backend\lib\gson-2.10.1.jar" (
    echo       Downloading gson-2.10.1.jar...
    curl.exe -f -s -L -o "backend\lib\gson-2.10.1.jar" "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
)

if not exist "backend\lib\postgresql-42.7.2.jar" (
    echo       Downloading postgresql-42.7.2.jar...
    curl.exe -f -s -L -o "backend\lib\postgresql-42.7.2.jar" "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar"
)
echo       Dependencies verified in backend\lib\
echo.

set JAVAC_EXE=javac
set JAVA_EXE=java

javac -version >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :HAS_JAVA

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        goto :HAS_JAVA
    )
)

for /d %%D in ("C:\Program Files\Java\jdk*" "C:\Program Files\Eclipse Adoptium\jdk*" "C:\Program Files\Microsoft\jdk*") do (
    if exist "%%D\bin\javac.exe" (
        set "JAVAC_EXE=%%D\bin\javac.exe"
        set "JAVA_EXE=%%D\bin\java.exe"
        goto :HAS_JAVA
    )
)

echo ====================================================================
echo [ERROR] Java compiler 'javac' was not found!
echo Please install JDK 17 or higher from https://adoptium.net
echo ====================================================================
pause
exit /b 1

:HAS_JAVA
echo [3/4] Compiling Java backend...
cd /d "%APP_DIR%backend"
if not exist "bin" mkdir "bin"

"%JAVAC_EXE%" -encoding UTF-8 -cp "lib\*" -d "bin" src\Main.java src\handlers\*.java src\middleware\*.java src\models\*.java src\repositories\*.java src\server\*.java src\utils\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo       Compilation successful!
echo.

echo [4/4] Starting server...
echo Application running on http://localhost:%PORT%
echo.

"%JAVA_EXE%" -cp "bin;lib\*" Main

pause
