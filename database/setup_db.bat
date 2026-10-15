@echo off
setlocal
echo ====================================================================
echo        Setting up PostgreSQL Database: leave_management
echo ====================================================================

set PGUSER=%DB_USER%
if "%PGUSER%"=="" set PGUSER=postgres

set PGPASSWORD=%DB_PASSWORD%
if "%PGPASSWORD%"=="" set PGPASSWORD=postgres

echo 1. Creating database if not exists...
psql -U %PGUSER% -c "CREATE DATABASE leave_management;" 2>nul || echo Database already exists or error ignored.

echo 2. Applying schema.sql...
psql -U %PGUSER% -d leave_management -f "%~dp0schema.sql"

echo 3. Applying seed.sql...
psql -U %PGUSER% -d leave_management -f "%~dp0seed.sql"

echo ====================================================================
echo Database setup completed!
echo ====================================================================
pause
