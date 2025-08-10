package com.yourcompany.distributor

import android.content.Intent
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
                        val url = URL("https://stellar-bienenstich-f651bf.netlify.app/app-version.json")
                        val connection = url.openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        val response = connection.getInputStream().bufferedReader().use { it.readText() }
                        JSONObject(response)
                    }
                    val versionCode = versionInfo.getInt("versionCode")
                    val versionName = versionInfo.getString("versionName")
                    // MainActivity currentVersionCode is 2; mirror logic here lightly
                    val isUpdate = versionCode > 2
                    if (isUpdate) {
                        updatesStatus.text = "Update available: v$versionName"
                    } else {
                        updatesStatus.text = "You're up to date"
                    }
                } catch (e: Exception) {
                    updatesStatus.text = "Failed to check updates"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
