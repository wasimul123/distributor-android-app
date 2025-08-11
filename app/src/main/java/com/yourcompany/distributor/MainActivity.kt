package com.yourcompany.distributor

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Environment
import android.widget.ProgressBar
import android.widget.TextView
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var cameraPhotoPath: String? = null
    
    // Activity result launchers
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    // Update functionality
    private val updateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null
    private val updateUrl = "https://stellar-bienenstitch-f651bf.netlify.app/app-version.json"
    private val apkDownloadUrl = "https://github.com/wasimul123/distributor-android-app/releases/download/v1.2.5/app-release.apk"
    private val currentVersionCode = 8 // Update this with each release
    private var updateCheckDone = false // Prevent multiple update checks

    companion object {
        private const val FILECHOOSER_RESULTCODE = 1
        private const val REQUEST_CAMERA_PERMISSION = 200
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loader = findViewById<View>(R.id.loader)
        webView = findViewById(R.id.webView)
        val refreshButton = findViewById<ImageButton>(R.id.refreshButton)
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)

        // Initialize activity result launchers
        setupActivityResultLaunchers()
        
        // Initialize update functionality
        try {
            setupUpdateReceiver()
        } catch (e: Exception) {
            // If update receiver setup fails, continue without auto-update
            Log.e("MainActivity", "Failed to setup update receiver: ${e.message}")
        }

        // Production URL - Netlify hosted app (correct domain)
        val url = "https://stellar-bienenstitch-f651bf.netlify.app/"

        // Configure WebView settings
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            allowContentAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION") 
            allowUniversalAccessFromFileURLs = true
            databaseEnabled = true
            
            // Enable file uploads
            mediaPlaybackRequiresUserGesture = false
        }

        // Configure scrollbars
        webView.isHorizontalScrollBarEnabled = true
        webView.isVerticalScrollBarEnabled = true
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        // Configure cookies
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        // Set up WebChromeClient for file uploads
        webView.webChromeClient = object : WebChromeClient() {
            // For Android 5.0+
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                // Note: We use our custom file chooser instead of the default intent
                try {
                    if (hasPermissions()) {
                        showFileChooserDialog()
                    } else {
                        requestPermissions()
                    }
                } catch (e: Exception) {
                    fileUploadCallback = null
                    Toast.makeText(this@MainActivity, "Cannot open file chooser", Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.grant(request.resources)
                }
            }
        }

        // Set up WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                loader.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loader.visibility = View.GONE
                super.onPageFinished(view, url)
                
                // No auto update prompt on page load. Badge-only check happens in onResume.
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                loader.visibility = View.GONE
                super.onReceivedError(view, errorCode, description, failingUrl)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }
        }

        // Set up refresh button
        refreshButton.setOnClickListener {
            webView.reload()
        }

        // Open settings screen
        settingsButton.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (_: Exception) { }
        }

        // Add long press to settings button for manual update check
        settingsButton.setOnLongClickListener {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
            updateCheckDone = false // Reset flag to allow update check
            checkForUpdates()
            true
        }

        // Load the URL
        loader.visibility = View.VISIBLE
        webView.loadUrl(url)

        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (this@MainActivity::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Check for updates and show dialog if available
        if (!updateCheckDone) {
            checkForUpdates()
            updateCheckDone = true
        } else {
            // Silent update check to toggle settings badge
            refreshUpdateBadge()
        }
    }

    private fun refreshUpdateBadge() {
        val badgeView = findViewById<View>(R.id.settingsBadge)
        updateScope.launch {
            try {
                val versionInfo = withContext(Dispatchers.IO) { checkServerVersion() }
                if (versionInfo != null && versionInfo.versionCode > currentVersionCode) {
                    badgeView?.visibility = View.VISIBLE
                } else {
                    badgeView?.visibility = View.GONE
                }
            } catch (_: Exception) {
                // On error, hide badge to avoid false positives
                badgeView?.visibility = View.GONE
            }
        }
    }

    private fun setupActivityResultLaunchers() {
        // File picker launcher
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleFilePickerResult(result)
        }

        // Camera launcher
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraResult(result)
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                showFileChooserDialog()
            } else {
                Toast.makeText(this, "Permissions required for file upload", Toast.LENGTH_LONG).show()
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = null
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        }
        permissionLauncher.launch(permissions)
    }

    private fun showFileChooserDialog() {
        val items = arrayOf("Choose File (CSV/Excel)", "Choose Any File", "Take Photo")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select File")
        builder.setItems(items) { _, which ->
            when (which) {
                0 -> openFilePicker() // Specific file types
                1 -> openAllFilesPicker() // All files fallback
                2 -> openCamera()
            }
        }
        builder.setOnCancelListener {
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = null
        }
        builder.show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        filePickerLauncher.launch(intent)
    }

    private fun openFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            
            // Comprehensive MIME types for CSV and Excel files
            val mimeTypes = arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/plain", // Some devices treat CSV as plain text
                "application/vnd.ms-excel", // .xls
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                "application/octet-stream" // Fallback for unrecognized files
            )
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            
            // Fallback: if the intent fails, try with broader type
            if (intent.resolveActivity(packageManager) != null) {
                filePickerLauncher.launch(intent)
            } else {
                // Fallback to file manager with all files
                openAllFilesPicker()
            }
        } catch (e: Exception) {
            // Ultimate fallback - simple file picker
            openAllFilesPicker()
        }
    }

    private fun openAllFilesPicker() {
        // Simple file picker that shows all files - works as fallback
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        filePickerLauncher.launch(intent)
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
            }

            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.yourcompany.distributor.fileprovider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(takePictureIntent)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir("Pictures")
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        cameraPhotoPath = image.absolutePath
        return image
    }

    private fun handleFilePickerResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                fileUploadCallback?.onReceiveValue(arrayOf(uri))
            } else {
                fileUploadCallback?.onReceiveValue(null)
            }
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    private fun handleCameraResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            if (cameraPhotoPath != null) {
                val file = File(cameraPhotoPath!!)
                val uri = Uri.fromFile(file)
                fileUploadCallback?.onReceiveValue(arrayOf(uri))
            } else {
                fileUploadCallback?.onReceiveValue(null)
            }
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
        cameraPhotoPath = null
    }

    // Update functionality
    private fun setupUpdateReceiver() {
        try {
            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            Log.d("MainActivity", "Download completed for ID: $id")
                            Toast.makeText(this@MainActivity, "Update downloaded! Installing...", Toast.LENGTH_SHORT).show()
                            installApk()
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error in download receiver: ${e.message}")
                        // If installation fails, show error
                        Toast.makeText(this@MainActivity, "Failed to process update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        } catch (e: Exception) {
            // If receiver registration fails, continue without auto-update
        }
    }

    private fun checkForUpdates() {
        updateScope.launch {
            try {
                Log.d("MainActivity", "Checking for updates...")
                val versionInfo = withContext(Dispatchers.IO) {
                    checkServerVersion()
                }
                
                if (versionInfo != null) {
                    Log.d("MainActivity", "Server version: ${versionInfo.versionCode}, Current version: $currentVersionCode")
                    if (versionInfo.versionCode > currentVersionCode) {
                        Log.d("MainActivity", "Update available! Showing dialog...")
                        showUpdateDialog(versionInfo)
                    } else {
                        Log.d("MainActivity", "No update available")
                        // Show toast for manual update checks
                        if (!updateCheckDone) {
                            Toast.makeText(this@MainActivity, "No updates available", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.d("MainActivity", "Failed to get version info from server")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking for updates: ${e.message}")
                // Silently fail - don't show error for update check
                // User can still use the app normally
            }
        }
    }

    private suspend fun checkServerVersion(): VersionInfo? {
        return try {
            Log.d("MainActivity", "Fetching version from: $updateUrl")
            // Bypass cached responses with a timestamp query param
            val cacheBustUrl = android.net.Uri.parse(updateUrl)
                .buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
                .toString()
            val url = URL(cacheBustUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000
            
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            Log.d("MainActivity", "Server response: $response")
            val json = JSONObject(response)
            
            VersionInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                downloadUrl = json.getString("downloadUrl"),
                releaseNotes = json.getString("releaseNotes")
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error fetching version info: ${e.message}")
            // If we can't check for updates, silently fail
            // This ensures the app still works without internet
            null
        }
    }

    private fun showUpdateDialog(versionInfo: VersionInfo) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("🎉 Update Available!")
        builder.setMessage(
            "New version ${versionInfo.versionName} is available!\n\n" +
            "What's new:\n${versionInfo.releaseNotes}\n\n" +
            "Would you like to download and install the update?"
        )
        builder.setPositiveButton("Update Now") { _, _ ->
            try {
                downloadUpdate(versionInfo.downloadUrl)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting download: ${e.message}")
                Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton("Later") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun downloadUpdate(url: String) {
        try {
            Log.d("MainActivity", "Starting download from: $url")
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Distributor App Update")
            request.setDescription("Downloading latest version...")
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "distributor-update.apk")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            Log.d("MainActivity", "Download started with ID: $downloadId")
            Toast.makeText(this, "Downloading update...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start download: ${e.message}")
            Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun installApk() {
        try {
            Log.d("MainActivity", "Attempting to install APK...")
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "distributor-update.apk")
            
            if (file.exists()) {
                Log.d("MainActivity", "APK file found, size: ${file.length()} bytes")
                val intent = Intent(Intent.ACTION_VIEW)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                }
                
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.d("MainActivity", "Install intent started")
            } else {
                Log.e("MainActivity", "APK file not found at: ${file.absolutePath}")
                Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to install update: ${e.message}")
            Toast.makeText(this, "Failed to install update: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    data class VersionInfo(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    override fun onDestroy() {
        super.onDestroy()
        try {
            downloadReceiver?.let { 
                unregisterReceiver(it)
                downloadReceiver = null
            }
        } catch (e: Exception) {
            // Ignore if receiver was not registered
        }
        updateScope.cancel()
        if (this::webView.isInitialized) {
            webView.destroy()
        }
    }
}