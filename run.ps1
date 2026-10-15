# Faculty Leave Management System — PowerShell Runner
$ErrorActionPreference = "Stop"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "       FACULTY LEAVE MANAGEMENT SYSTEM" -ForegroundColor Cyan
Write-Host "       Semester III Industry Practice-I Project" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

$AppDir = $PSScriptRoot
Set-Location $AppDir

$env:PORT = if ($env:PORT) { $env:PORT } else { "8080" }

Write-Host "[1/4] Environment Config:" -ForegroundColor Yellow
Write-Host "      PORT: $env:PORT"
Write-Host "      DB: Managed via db.properties (Supabase / PostgreSQL)"
Write-Host ""

# 1. Ensure backend/lib exists
$LibDir = Join-Path $AppDir "backend\lib"
if (-not (Test-Path $LibDir)) {
    New-Item -ItemType Directory -Path $LibDir -Force | Out-Null
}

# 2. Check dependencies
Write-Host "[2/4] Checking dependencies..." -ForegroundColor Yellow
$GsonJar = Join-Path $LibDir "gson-2.10.1.jar"
$PgJar = Join-Path $LibDir "postgresql-42.7.2.jar"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

if (-not (Test-Path $GsonJar)) {
    Write-Host "      Downloading gson-2.10.1.jar..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" -OutFile $GsonJar
}

if (-not (Test-Path $PgJar)) {
    Write-Host "      Downloading postgresql-42.7.2.jar..."
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar" -OutFile $PgJar
}

Write-Host "      Dependencies verified in backend\lib\" -ForegroundColor Green
Write-Host ""

# 3. Locate Java compiler
$Javac = Get-Command "javac" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1

if (-not $Javac -and $env:JAVA_HOME) {
    $Candidate = Join-Path $env:JAVA_HOME "bin\javac.exe"
    if (Test-Path $Candidate) { $Javac = $Candidate }
}

if (-not $Javac) {
    $SearchPaths = @(
        "C:\Program Files\Java\jdk*\bin\javac.exe",
        "C:\Program Files\Eclipse Adoptium\jdk*\bin\javac.exe",
        "C:\Program Files\Microsoft\jdk*\bin\javac.exe",
        "C:\Program Files\Amazon Corretto\jdk*\bin\javac.exe"
    )
    foreach ($Pattern in $SearchPaths) {
        $Found = Get-Item $Pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($Found) { $Javac = $Found.FullName; break }
    }
}

if (-not $Javac) {
    Write-Host "====================================================================" -ForegroundColor Red
    Write-Host "[ERROR] Java compiler 'javac' was not found!" -ForegroundColor Red
    Write-Host "Please install JDK 17 or higher from https://adoptium.net"
    Write-Host "====================================================================" -ForegroundColor Red
    exit 1
}

$JavaDir = Split-Path $Javac
$Java = Join-Path $JavaDir "java.exe"

# 4. Compile
Write-Host "[3/4] Compiling Java backend..." -ForegroundColor Yellow
$BackendDir = Join-Path $AppDir "backend"
$BinDir = Join-Path $BackendDir "bin"
if (-not (Test-Path $BinDir)) {
    New-Item -ItemType Directory -Path $BinDir -Force | Out-Null
}

Set-Location $BackendDir

$SourceFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

& $Javac -encoding UTF-8 -cp "lib/*" -d "bin" $SourceFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    Set-Location $AppDir
    exit 1
}

Write-Host "      Compilation successful!" -ForegroundColor Green
Write-Host ""

# 5. Run Server
Write-Host "[4/4] Starting server..." -ForegroundColor Cyan
Write-Host "Application will open at http://localhost:$env:PORT" -ForegroundColor Cyan
Write-Host ""

& $Java -cp "bin;lib/*" Main

Set-Location $AppDir
