# Quick Start Guide - Android In-App Updates

## 🚀 Get Started in 5 Minutes

### 1. Install Git
Download and install Git from: https://git-scm.com/downloads

### 2. Run Setup Scripts
Double-click these files in order:
1. `setup-git.bat` - Sets up Git repository
2. `build-and-release.bat 2 1.1.0` - Builds new version

### 3. Create GitHub Repository
1. Go to [GitHub.com](https://github.com)
2. Click "New repository"
3. Name: `distributor-android-app`
4. Make it **Public**
5. Don't initialize with README

### 4. Connect to GitHub
```bash
git remote add origin https://github.com/YOUR_USERNAME/distributor-android-app.git
git branch -M main
git push -u origin main
```

### 5. Create First Release
1. Go to your repository on GitHub
2. Click "Releases" → "Create a new release"
3. Tag: `v1.0.0`
4. Title: `Version 1.0.0 - Initial Release`
5. Upload the APK from `app/build/outputs/apk/release/`
6. Publish release

### 6. Host Version JSON
Create `app-version.json` on your web server or use Netlify:

```json
{
  "versionCode": 2,
  "versionName": "1.1.0",
  "downloadUrl": "https://github.com/YOUR_USERNAME/distributor-android-app/releases/download/v1.1.0/app-release.apk",
  "releaseNotes": "• Bug fixes and improvements",
  "forceUpdate": false
}
```

### 7. Update URLs in Code
In `MainActivity.kt`, update:
- `updateUrl`: Your version JSON URL
- `apkDownloadUrl`: Your GitHub releases URL

### 8. Test Updates
1. Install version 1.0.0
2. Update version JSON to 1.1.0
3. Launch app - should show update dialog
4. Test download and installation

## 📱 How It Works

1. **App Launch**: Checks for updates automatically
2. **Version Check**: Compares local vs server version
3. **Update Dialog**: Shows user update information
4. **Download**: Downloads APK in background
5. **Install**: Prompts user to install update
6. **Restart**: App restarts with new version

## 🔧 Customization

- Edit `update-config.json` for settings
- Modify dialog text in `MainActivity.kt`
- Change update check frequency
- Add custom update logic

## 🚨 Troubleshooting

- **Update not showing**: Check version codes and JSON format
- **Download fails**: Verify URL accessibility
- **Installation fails**: Check APK signature
- **Git issues**: Run `setup-git.bat` again

## 📞 Support

- Check Android logs: `adb logcat`
- Verify network connectivity
- Test with different devices
- Review permission settings

## 🎯 Next Steps

1. Test the update system thoroughly
2. Set up automated builds with GitHub Actions
3. Implement rollback mechanisms
4. Add analytics and monitoring
5. Plan your release strategy

---

**Need help?** Check the full `UPDATE_SYSTEM_README.md` for detailed instructions.
