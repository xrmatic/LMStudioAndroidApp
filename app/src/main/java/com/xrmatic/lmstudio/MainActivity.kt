package com.xrmatic.lmstudio

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xrmatic.lmstudio.prefs.AppPreferences

/**
 * Launcher activity – routes to the appropriate screen depending on whether
 * the user has already configured a server connection.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = AppPreferences(this)

        val destination = if (prefs.isConfigured) {
            ChatActivity::class.java
        } else {
            SettingsActivity::class.java
        }

        startActivity(Intent(this, destination))
        finish()
    }
}
