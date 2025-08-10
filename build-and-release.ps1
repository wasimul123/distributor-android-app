# Android App Build and Release Script
Write-Host "========================================" -ForegroundColor Green
Write-Host "Android App Build and Release Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Check if version arguments are provided
if ($args.Count -lt 2) {
    Write-Host "Usage: .\build-and-release.ps1 [version_code] [version_name]" -ForegroundColor Red
    Write-Host "Example: .\build-and-release.ps1 2 1.1.0" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

$VERSION_CODE = $args[0]
$VERSION_NAME = $args[1]

Write-Host "Building version $VERSION_NAME (code: $VERSION_CODE)" -ForegroundColor Yellow
Write-Host ""

# Update version in build.gradle
Write-Host "Updating version in build.gradle..." -ForegroundColor Cyan
$buildGradlePath = "app/build.gradle"
if (Test-Path $buildGradlePath) {
    $content = Get-Content $buildGradlePath -Raw
    $content = $content -replace 'versionCode \d+', "versionCode $VERSION_CODE"
    $content = $content -replace 'versionName "[^"]*"', "versionName `"$VERSION_NAME`""
    Set-Content $buildGradlePath $content -Encoding UTF8
    Write-Host "✓ build.gradle updated" -ForegroundColor Green
} else {
    Write-Host "✗ build.gradle not found!" -ForegroundColor Red
}

# Update version in MainActivity.kt
Write-Host "Updating version in MainActivity.kt..." -ForegroundColor Cyan
$mainActivityPath = "app/src/main/java/com/yourcompany/distributor/MainActivity.kt"
if (Test-Path $mainActivityPath) {
    $content = Get-Content $mainActivityPath -Raw
    $content = $content -replace 'private val currentVersionCode = \d+', "private val currentVersionCode = $VERSION_CODE"
    Set-Content $mainActivityPath $content -Encoding UTF8
    Write-Host "✓ MainActivity.kt updated" -ForegroundColor Green
} else {
    Write-Host "✗ MainActivity.kt not found!" -ForegroundColor Red
}

# Clean previous builds
Write-Host "Cleaning previous builds..." -ForegroundColor Cyan
try {
    & .\gradlew clean
    Write-Host "✓ Clean completed" -ForegroundColor Green
} catch {
    Write-Host "✗ Clean failed: $_" -ForegroundColor Red
}

# Build release APK
Write-Host "Building release APK..." -ForegroundColor Cyan
try {
    & .\gradlew assembleRelease
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Build completed successfully!" -ForegroundColor Green
    } else {
        Write-Host "✗ Build failed!" -ForegroundColor Red
        Read-Host "Press Enter to continue"
        exit 1
    }
} catch {
    Write-Host "✗ Build failed: $_" -ForegroundColor Red
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Build completed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Test the APK: app/build/outputs/apk/release/app-release.apk" -ForegroundColor White
Write-Host "2. Create GitHub release with tag: v$VERSION_NAME" -ForegroundColor White
Write-Host "3. Upload the APK to the release" -ForegroundColor White
Write-Host "4. Update your version JSON file" -ForegroundColor White
Write-Host "5. Commit and push changes to Git" -ForegroundColor White
Write-Host ""
Write-Host "APK location: app/build/outputs/apk/release/app-release.apk" -ForegroundColor Cyan
Write-Host ""

# Open the output directory
$outputDir = "app\build\outputs\apk\release"
if (Test-Path $outputDir) {
    Write-Host "Opening output directory..." -ForegroundColor Cyan
    Start-Process "explorer.exe" -ArgumentList $outputDir
} else {
    Write-Host "Output directory not found: $outputDir" -ForegroundColor Red
}

Read-Host "Press Enter to continue"
