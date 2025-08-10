@echo off
echo ========================================
echo Git Repository Setup Script
echo ========================================

REM Check if Git is installed
git --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Git is not installed or not in PATH.
    echo Please install Git from: https://git-scm.com/downloads
    echo After installation, restart this script.
    pause
    exit /b 1
)

echo Git is installed. Version:
git --version
echo.

REM Check if already a Git repository
if exist ".git" (
    echo This directory is already a Git repository.
    echo.
    echo Current status:
    git status
    echo.
    echo To continue setup, run: git remote add origin YOUR_REPO_URL
    pause
    exit /b 0
)

REM Initialize Git repository
echo Initializing Git repository...
git init

REM Add all files
echo Adding files to Git...
git add .

REM Initial commit
echo Creating initial commit...
git commit -m "Initial commit - Distributor Android App v1.0.0"

echo.
echo ========================================
echo Git repository initialized successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Create a new repository on GitHub
echo 2. Copy the repository URL
echo 3. Run: git remote add origin YOUR_REPO_URL
echo 4. Run: git branch -M main
echo 5. Run: git push -u origin main
echo.
echo Example:
echo git remote add origin https://github.com/username/distributor-android-app.git
echo git branch -M main
echo git push -u origin main
echo.

pause
