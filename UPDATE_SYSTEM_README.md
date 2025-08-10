# Android In-App Update System Setup Guide

## Overview
This guide will help you set up a complete Git-based in-app update system for your Android app that works post-launch.

## Prerequisites
1. Install Git from: https://git-scm.com/downloads
2. GitHub account (free)
3. Android Studio or command line build tools

## Step 1: Git Repository Setup

### 1.1 Initialize Git Repository
```bash
cd android-app
git init
git add .
git commit -m "Initial commit"
```

### 1.2 Create GitHub Repository
1. Go to GitHub.com and create a new repository
2. Name it: `distributor-android-app`
3. Make it public (for easier access)
4. Don't initialize with README (we already have files)

### 1.3 Connect Local to Remote
```bash
git remote add origin https://github.com/YOUR_USERNAME/distributor-android-app.git
git branch -M main
git push -u origin main
```

## Step 2: GitHub Releases Setup

### 2.1 Create Release
1. Go to your GitHub repository
2. Click "Releases" → "Create a new release"
3. Tag: `v1.0.0`
4. Title: `Version 1.0.0 - Initial Release`
5. Description: Add release notes
6. Upload your APK file
7. Publish release

### 2.2 Update Version Info
For each new release:
1. Update `versionCode` in `app/build.gradle`
2. Update `versionName` in `app/build.gradle`
3. Update `currentVersionCode` in `MainActivity.kt`
4. Build new APK
5. Create new GitHub release

## Step 3: Update Server Configuration

### 3.1 Create Version JSON File
Create a file at: `https://your-domain.com/app-version.json`

```json
{
  "versionCode": 2,
  "versionName": "1.1.0",
  "downloadUrl": "https://github.com/YOUR_USERNAME/distributor-android-app/releases/download/v1.1.0/distributor-v1.1.0.apk",
  "releaseNotes": "• Bug fixes and performance improvements\n• Enhanced file upload functionality\n• Better error handling",
  "forceUpdate": false,
  "minVersionCode": 1
}
```

### 3.2 Update MainActivity.kt URLs
Replace these URLs in your MainActivity.kt:
- `updateUrl`: Your version JSON file URL
- `apkDownloadUrl`: Your GitHub releases URL

## Step 4: Build and Release Process

### 4.1 Build APK
```bash
./gradlew assembleRelease
```

### 4.2 Sign APK (Required for updates)
1. Generate keystore:
```bash
keytool -genkey -v -keystore distributor.keystore -alias distributor -keyalg RSA -keysize 2048 -validity 10000
```

2. Add to `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("distributor.keystore")
            storePassword "your_store_password"
            keyAlias "distributor"
            keyPassword "your_key_password"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 4.3 Release Process
1. Update version numbers
2. Build signed APK
3. Test APK
4. Create GitHub release
5. Upload APK to release
6. Update version JSON file
7. Commit and push changes

## Step 5: Testing Updates

### 5.1 Test Update Flow
1. Install version 1.0.0
2. Update version JSON to version 1.1.0
3. Launch app - should show update dialog
4. Test download and installation

### 5.2 Debug Update Issues
- Check logcat for update-related errors
- Verify URLs are accessible
- Check file permissions
- Verify APK signature

## Step 6: Production Deployment

### 6.1 Host Version JSON
- Use Netlify, Vercel, or your own server
- Ensure HTTPS is enabled
- Set proper CORS headers if needed

### 6.2 Monitor Updates
- Track download counts in GitHub releases
- Monitor crash reports
- User feedback collection

## Troubleshooting

### Common Issues
1. **Update not showing**: Check version codes and JSON format
2. **Download fails**: Verify URL accessibility and file size
3. **Installation fails**: Check APK signature and permissions
4. **App crashes**: Verify update receiver registration

### Debug Commands
```bash
# Check current version
adb shell dumpsys package com.yourcompany.distributor | grep versionCode

# Install APK manually
adb install -r app-release.apk

# Check logs
adb logcat | grep -i update
```

## Security Considerations
1. Always use HTTPS for update URLs
2. Verify APK signatures
3. Implement integrity checks
4. Rate limit update checks
5. Validate JSON responses

## Best Practices
1. Test updates thoroughly before release
2. Provide clear release notes
3. Implement rollback mechanisms
4. Monitor update success rates
5. Plan for emergency updates

## Support
For issues or questions:
1. Check Android logs
2. Verify network connectivity
3. Test with different devices
4. Review permission settings
