package com.yourcompany.distributor

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
// Removed androidx.preference to avoid extra dependency; using standard SharedPreferences
import org.json.JSONObject
import java.net.URL
import kotlinx.coroutines.*
import android.app.AlertDialog

class SettingsActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        val apiPassword = findViewById<EditText>(R.id.apiPassword)
        val adminPassword = findViewById<EditText>(R.id.adminPassword)
        val saveBtn = findViewById<Button>(R.id.saveButton)
        val checkUpdatesBtn = findViewById<Button>(R.id.checkUpdatesButton)
        val updatesStatus = findViewById<TextView>(R.id.updateStatus)
        val autoCheckSwitch = findViewById<Switch>(R.id.autoCheckSwitch)

        apiPassword.setText(prefs.getString("apiPassword", ""))
        adminPassword.setText(prefs.getString("adminPassword", ""))
        autoCheckSwitch.isChecked = prefs.getBoolean("autoCheckUpdates", false)

        saveBtn.setOnClickListener {
            prefs.edit()
                .putString("apiPassword", apiPassword.text.toString())
                .putString("adminPassword", adminPassword.text.toString())
                .putBoolean("autoCheckUpdates", autoCheckSwitch.isChecked)
                .apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        checkUpdatesBtn.setOnClickListener {
            updatesStatus.text = "Checking..."
            scope.launch {
                try {
                    val versionInfo = withContext(Dispatchers.IO) {
                        val url = URL("https://stellar-bienenstitch-f651bf.netlify.app/app-version.json")
                        val connection = url.openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        val response = connection.getInputStream().bufferedReader().use { it.readText() }
                        JSONObject(response)
                    }
                    val versionCode = versionInfo.optInt("versionCode", 0)
                    val versionName = versionInfo.optString("versionName", "")
                    val downloadUrl = versionInfo.optString("downloadUrl", "")

                    val currentVersionCode = 8 // keep in sync with MainActivity
                    val isUpdate = versionCode > currentVersionCode && downloadUrl.isNotBlank()

                    if (isUpdate) {
                        updatesStatus.text = "Update found: v$versionName. Click Yes to proceed."
                        AlertDialog.Builder(this@SettingsActivity)
                            .setTitle("Update found")
                            .setMessage("Version $versionName is available. Do you want to download and install it now?")
                            .setPositiveButton("Yes") { _, _ ->
                                downloadUpdate(downloadUrl)
                            }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        updatesStatus.text = "No updates found"
                        Toast.makeText(this@SettingsActivity, "No updates found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    updatesStatus.text = "No updates found"
                    Toast.makeText(this@SettingsActivity, "No updates found", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun downloadUpdate(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Distributor App Update")
            request.setDescription("Downloading latest version...")
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "distributor-update.apk")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Downloading update...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start download", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
