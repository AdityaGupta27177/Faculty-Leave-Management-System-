# Faculty Leave Management System — Push to GitHub
$ErrorActionPreference = "Continue"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "       PUSHING FACULTY LEAVE MANAGEMENT SYSTEM TO GITHUB" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $PSScriptRoot

if (-not (Get-Command "git" -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Git is not installed or not found in PATH." -ForegroundColor Red
    Write-Host "Please install Git from https://git-scm.com/"
    exit 1
}

if (-not (Test-Path ".git")) {
    Write-Host "[1/5] Initializing Git repository..." -ForegroundColor Yellow
    git init
} else {
    Write-Host "[1/5] Git repository already initialized." -ForegroundColor Green
}

Write-Host "[2/5] Setting branch to main..." -ForegroundColor Yellow
git branch -M main

Write-Host "[3/5] Configuring remote origin..." -ForegroundColor Yellow
git remote remove origin 2>$null
git remote add origin https://github.com/AdityaGupta27177/Faculty-Leave-Management-System-.git

Write-Host "[4/5] Staging all files..." -ForegroundColor Yellow
git add .
git commit -m "Transform Faculty Leave Management System: bespoke editorial UI, complete full-stack implementation"

Write-Host "[5/5] Pushing to GitHub..." -ForegroundColor Cyan
git push -u origin main

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Pulling remote changes before retrying push..." -ForegroundColor Yellow
    git pull origin main --rebase --allow-unrelated-histories
    git push -u origin main
}

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "====================================================================" -ForegroundColor Green
    Write-Host "SUCCESS! All files pushed to: https://github.com/AdityaGupta27177/Faculty-Leave-Management-System-" -ForegroundColor Green
    Write-Host "====================================================================" -ForegroundColor Green
}
