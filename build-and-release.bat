@echo off
echo ========================================
echo Android App Build and Release Script
echo ========================================

REM Check if version arguments are provided
if "%1"=="" (
    echo Usage: build-and-release.bat [version_code] [version_name]
    echo Example: build-and-release.bat 2 1.1.0
    pause
    exit /b 1
)

set VERSION_CODE=%1
set VERSION_NAME=%2

echo Building version %VERSION_NAME% (code: %VERSION_CODE%)
echo.

REM Update version in build.gradle
echo Updating version in build.gradle...
powershell -Command "(Get-Content 'app/build.gradle') -replace 'versionCode \d+', 'versionCode %VERSION_CODE%' | Out-File -FilePath 'app/build.gradle' -Encoding UTF8"
powershell -Command "(Get-Content 'app/build.gradle') -replace 'versionName \"[^\"]*\"', 'versionName \"%VERSION_NAME%\"' | Out-File -FilePath 'app/build.gradle' -Encoding UTF8"

REM Update version in MainActivity.kt
echo Updating version in MainActivity.kt...
powershell -Command "(Get-Content 'app/src/main/java/com/yourcompany/distributor/MainActivity.kt') -replace 'private val currentVersionCode = \d+', 'private val currentVersionCode = %VERSION_CODE%' | Out-File -FilePath 'app/src/main/java/com/yourcompany/distributor/MainActivity.kt' -Encoding UTF8"

REM Clean previous builds
echo Cleaning previous builds...
call gradlew clean

REM Build release APK
echo Building release APK...
call gradlew assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Test the APK: app/build/outputs/apk/release/app-release.apk
echo 2. Create GitHub release with tag: v%VERSION_NAME%
echo 3. Upload the APK to the release
echo 4. Update your version JSON file
echo 5. Commit and push changes to Git
echo.
echo APK location: app/build/outputs/apk/release/app-release.apk
echo.

REM Open the output directory
explorer "app\build\outputs\apk\release"

pause
