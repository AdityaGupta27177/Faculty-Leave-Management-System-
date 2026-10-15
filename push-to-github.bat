@echo off
title Push to GitHub — Faculty Leave Management System
cd /d "%~dp0"

echo ====================================================================
echo        PUSHING FACULTY LEAVE MANAGEMENT SYSTEM TO GITHUB
echo ====================================================================
echo.

git --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Git is not installed or not in PATH!
    echo Please install Git from https://git-scm.com/
    pause
    exit /b 1
)

if not exist ".git" (
    echo [1/5] Initializing Git repository...
    git init
) else (
    echo [1/5] Git repository already initialized.
)

echo [2/5] Setting default branch to main...
git branch -M main

echo [3/5] Setting remote origin...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/AdityaGupta27177/Faculty-Leave-Management-System-.git

echo [4/5] Staging files and committing...
git add .
git commit -m "Transform Faculty Leave Management System: bespoke editorial UI, complete full-stack implementation"

echo [5/5] Pushing to GitHub (https://github.com/AdityaGupta27177/Faculty-Leave-Management-System-)...
echo.
git push -u origin main

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo --------------------------------------------------------------------
    echo If push was rejected because remote has existing README/commits,
    echo attempting to pull and rebase before pushing...
    echo --------------------------------------------------------------------
    git pull origin main --rebase --allow-unrelated-histories
    git push -u origin main
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================================================
    echo SUCCESS! All code pushed to:
    echo https://github.com/AdityaGupta27177/Faculty-Leave-Management-System-
    echo ====================================================================
) else (
    echo.
    echo ====================================================================
    echo Notice: If prompted, please sign in with your GitHub credentials or
    echo Personal Access Token in the browser window/popup that opens.
    echo ====================================================================
)

echo.
pause
