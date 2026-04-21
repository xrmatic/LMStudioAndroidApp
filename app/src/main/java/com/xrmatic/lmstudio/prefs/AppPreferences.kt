package com.xrmatic.lmstudio.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Centralised access to persisted user preferences.
 *
 * Sensitive values (API key) are stored in [EncryptedSharedPreferences] backed
 * by the Android Keystore.  Non-sensitive settings use regular [SharedPreferences].
 */
class AppPreferences(context: Context) {

    // ── Regular preferences ────────────────────────────────────────────────

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Encrypted preferences (API key) ───────────────────────────────────

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Connection settings ────────────────────────────────────────────────

    /** Full server URL, e.g. "http://192.168.1.100:1234" or "https://my-server:1234" */
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    /** API key (empty string = no auth). Stored encrypted. */
    var apiKey: String
        get() = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_API_KEY, value).apply()

    /** When true, restrict all requests to WiFi; block on mobile data. */
    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    /**
     * When true, accept self-signed / user-installed TLS certificates.
     * Default is false (only system CAs trusted).
     */
    var allowSelfSignedCerts: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_SELF_SIGNED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_SELF_SIGNED, value).apply()

    /** Last model the user selected; empty = use the first available model. */
    var selectedModel: String
        get() = prefs.getString(KEY_SELECTED_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    /** System prompt sent at the beginning of every new conversation. */
    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    /** Sampling temperature [0.0 – 2.0]. */
    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    /** Whether to log HTTP traffic (debug aid; only used in debug builds). */
    var enableVerboseLogging: Boolean
        get() = prefs.getBoolean(KEY_VERBOSE_LOGGING, false)
        set(value) = prefs.edit().putBoolean(KEY_VERBOSE_LOGGING, value).apply()

    /** Returns true if the user has completed initial setup. */
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && serverUrl != DEFAULT_SERVER_URL

    companion object {
        private const val PREFS_NAME = "lmstudio_prefs"
        private const val ENCRYPTED_PREFS_NAME = "lmstudio_secure_prefs"

        const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_ALLOW_SELF_SIGNED = "allow_self_signed"
        const val KEY_SELECTED_MODEL = "selected_model"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_VERBOSE_LOGGING = "verbose_logging"

        private const val DEFAULT_SERVER_URL = ""
        private const val DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant."
        const val DEFAULT_TEMPERATURE = 0.7f
    }
}
