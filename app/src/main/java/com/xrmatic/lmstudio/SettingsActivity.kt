package com.xrmatic.lmstudio

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xrmatic.lmstudio.api.LMStudioClient
import com.xrmatic.lmstudio.databinding.ActivitySettingsBinding
import com.xrmatic.lmstudio.prefs.AppPreferences
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        prefs = AppPreferences(this)

        populateFields()
        setupListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ── Populate from stored preferences ───────────────────────────────────

    private fun populateFields() {
        binding.etServerUrl.setText(prefs.serverUrl)
        binding.etApiKey.setText(prefs.apiKey)
        binding.switchWifiOnly.isChecked = prefs.wifiOnly
        binding.switchSelfSigned.isChecked = prefs.allowSelfSignedCerts
        binding.switchVerboseLogging.isChecked = prefs.enableVerboseLogging
        binding.etSystemPrompt.setText(prefs.systemPrompt)

        val tempProgress = ((prefs.temperature / 2.0f) * 100).toInt()
        binding.sliderTemperature.progress = tempProgress
        updateTemperatureLabel(prefs.temperature)

        // Warn if SSL is disabled
        updateSslWarning()
    }

    // ── Listeners ──────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveAndContinue() }
        binding.btnTestConnection.setOnClickListener { testConnection() }

        binding.switchSelfSigned.setOnCheckedChangeListener { _, checked ->
            updateSslWarning(checked)
        }

        binding.sliderTemperature.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val temp = (progress / 100.0f) * 2.0f
                updateTemperatureLabel(temp)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        // Show/hide cleartext warning based on URL scheme
        binding.etServerUrl.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val url = s?.toString().orEmpty().trim().lowercase()
                binding.tvHttpWarning.visibility =
                    if (url.startsWith("http://")) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    // ── Save ───────────────────────────────────────────────────────────────

    private fun saveAndContinue() {
        val url = binding.etServerUrl.text.toString().trim()
        if (url.isBlank()) {
            binding.tilServerUrl.error = getString(R.string.error_server_url_empty)
            return
        }
        binding.tilServerUrl.error = null

        prefs.serverUrl = url
        prefs.apiKey = binding.etApiKey.text.toString().trim()
        prefs.wifiOnly = binding.switchWifiOnly.isChecked
        prefs.allowSelfSignedCerts = binding.switchSelfSigned.isChecked
        prefs.enableVerboseLogging = binding.switchVerboseLogging.isChecked
        prefs.systemPrompt = binding.etSystemPrompt.text.toString()

        val temp = (binding.sliderTemperature.progress / 100.0f) * 2.0f
        prefs.temperature = temp

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()

        // Navigate to chat
        startActivity(Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    // ── Test connection ────────────────────────────────────────────────────

    private fun testConnection() {
        val url = binding.etServerUrl.text.toString().trim()
        if (url.isBlank()) {
            binding.tilServerUrl.error = getString(R.string.error_server_url_empty)
            return
        }

        // Build a one-shot TestConfig from current form values – does NOT touch
        // the persisted AppPreferences so no side-effects on the stored settings.
        val testConfig = TestConnectionConfig(
            serverUrl = url,
            apiKey = binding.etApiKey.text.toString().trim(),
            allowSelfSignedCerts = binding.switchSelfSigned.isChecked
        )

        binding.btnTestConnection.isEnabled = false
        binding.progressTestConnection.visibility = View.VISIBLE
        binding.tvConnectionStatus.visibility = View.GONE

        lifecycleScope.launch {
            val (success, message) = try {
                val service = LMStudioClient.buildForTest(testConfig)
                val response = service.listModels()
                if (response.isSuccessful) {
                    val count = response.body()?.data?.size ?: 0
                    Pair(true, getString(R.string.connection_success, count))
                } else {
                    Pair(false, getString(R.string.connection_failed_http, response.code()))
                }
            } catch (e: Exception) {
                Pair(false, getString(R.string.connection_failed_error, e.message))
            }

            binding.btnTestConnection.isEnabled = true
            binding.progressTestConnection.visibility = View.GONE
            binding.tvConnectionStatus.visibility = View.VISIBLE
            binding.tvConnectionStatus.text = message
            binding.tvConnectionStatus.setTextColor(
                getColor(if (success) R.color.success_text else R.color.error_text)
            )
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun updateTemperatureLabel(temp: Float) {
        binding.tvTemperatureValue.text = getString(R.string.temperature_value, temp)
    }

    private fun updateSslWarning(checked: Boolean = binding.switchSelfSigned.isChecked) {
        binding.tvSelfSignedWarning.visibility = if (checked) View.VISIBLE else View.GONE
    }
}

/** Lightweight value object used only for the one-shot connection test. */
data class TestConnectionConfig(
    val serverUrl: String,
    val apiKey: String,
    val allowSelfSignedCerts: Boolean
)
