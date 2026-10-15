#!/usr/bin/env bash
set -e

echo "===================================================================="
echo "       FACULTY LEAVE MANAGEMENT SYSTEM"
echo "       Semester III Industry Practice-I Project"
echo "===================================================================="
echo ""

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR"

export PORT="${PORT:-8080}"
# DB connection is loaded automatically from db.properties (Supabase or local PostgreSQL)

mkdir -p backend/lib backend/bin

echo "[1/3] Checking dependencies..."
if [ ! -f "backend/lib/gson-2.10.1.jar" ]; then
    echo "      Downloading gson-2.10.1.jar..."
    curl -sLo backend/lib/gson-2.10.1.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
fi

if [ ! -f "backend/lib/postgresql-42.7.2.jar" ]; then
    echo "      Downloading postgresql-42.7.2.jar..."
    curl -sLo backend/lib/postgresql-42.7.2.jar https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar
fi

echo "[2/3] Compiling backend..."
find backend/src -name "*.java" > backend/sources.txt
javac -encoding UTF-8 -cp "backend/lib/*" -d backend/bin @backend/sources.txt
rm backend/sources.txt

echo "[3/3] Starting server on http://localhost:${PORT}..."
java -cp "backend/bin:backend/lib/*" Main
