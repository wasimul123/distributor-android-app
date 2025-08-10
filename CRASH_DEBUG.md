# 🚨 Android App Crash Debug Guide

## 🔍 **Quick Fix for App Crashes**

If the app is crashing after installing, follow these steps to debug:

### **Step 1: Disable Auto-Update (Immediate Fix)**
Temporarily disable auto-update to isolate the issue:

```kotlin
// In MainActivity.kt, comment out these lines:
// setupUpdateReceiver()
// checkForUpdates() 
```

### **Step 2: Check Logcat**
Use Android Studio or ADB to see crash logs:
```bash
adb logcat | grep -i "crash\|error\|exception"
```

### **Step 3: Common Crash Causes**

#### **1. Permission Issues**:
- Missing storage permissions
- Camera permissions not granted
- Install unknown apps permission

#### **2. Memory Issues**:
- WebView not properly initialized
- Coroutines not properly managed
- BroadcastReceiver registration issues

#### **3. Network Issues**:
- Update URL not accessible
- JSON parsing errors
- Timeout issues

### **Step 4: Safe Build Configuration**

Create a version WITHOUT auto-update for testing:

#### **MainActivity.kt Changes**:
```kotlin
@SuppressLint("SetJavaScriptEnabled")
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val loader = findViewById<View>(R.id.loader)
    webView = findViewById(R.id.webView)
    val refreshButton = findViewById<ImageButton>(R.id.refreshButton)

    // Initialize activity result launchers
    setupActivityResultLaunchers()
    
    // DISABLE AUTO-UPDATE FOR TESTING
    // setupUpdateReceiver()

    // Production URL
    val url = "https://stellar-bienenstitch-f651bf.netlify.app/"

    // Rest of WebView setup...
}
```

#### **And in onPageFinished**:
```kotlin
override fun onPageFinished(view: WebView?, url: String?) {
    loader.visibility = View.GONE
    super.onPageFinished(view, url)
    
    // DISABLE UPDATE CHECK FOR TESTING
    // checkForUpdates()
}
```

### **Step 5: Build & Test**
```bash
cd android-app
./gradlew clean
./gradlew assembleDebug
```

### **Step 6: Re-enable Features Gradually**
Once the basic app works:

1. **Test File Upload** - Ensure CSV/Excel upload works
2. **Test WebView** - Ensure website loads properly  
3. **Add Auto-Update** - Re-enable one feature at a time

---

## 🛠️ **Advanced Debugging**

### **Enable Debug Mode**:
```kotlin
// Add to MainActivity.onCreate()
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

### **Add Crash Logging**:
```kotlin
// Add try-catch around entire onCreate
override fun onCreate(savedInstanceState: Bundle?) {
    try {
        super.onCreate(savedInstanceState)
        // ... rest of code
    } catch (e: Exception) {
        Log.e("MainActivity", "Fatal error in onCreate", e)
        Toast.makeText(this, "App initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        finish()
    }
}
```

### **Test Without Network**:
- Disable WiFi/mobile data
- Launch app to test offline behavior
- Auto-update should fail gracefully

---

## 📱 **Working Version Configuration**

Here's a minimal, crash-free configuration:

### **build.gradle** (Minimal dependencies):
```gradle
dependencies {
    implementation 'androidx.core:core-ktx:1.13.1'
    implementation 'androidx.activity:activity-ktx:1.9.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    // Remove coroutines if not needed for basic functionality
}
```

### **AndroidManifest.xml** (Essential permissions only):
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<!-- Remove update-related permissions for testing -->
```

---

## ✅ **Success Checklist**

- [ ] App launches without crashing
- [ ] WebView loads the website  
- [ ] File upload button responds
- [ ] CSV/Excel files can be selected
- [ ] Website features work in WebView
- [ ] Back button navigation works
- [ ] App can be closed and reopened

Once all basic features work, gradually re-enable auto-update.

---

**🎯 Priority: Get basic app working first, then add advanced features!**
