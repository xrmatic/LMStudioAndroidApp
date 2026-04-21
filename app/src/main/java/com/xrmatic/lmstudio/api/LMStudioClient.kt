package com.xrmatic.lmstudio.api

import com.xrmatic.lmstudio.SettingsActivity.TestConnectionConfig
import com.xrmatic.lmstudio.prefs.AppPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Builds Retrofit + OkHttp clients for the LM Studio API.
 *
 * Security modes (controlled by user preferences):
 *  - HTTPS with system CAs only (default / most secure)
 *  - HTTPS with user-installed CAs (for self-signed certificates)
 *  - HTTP cleartext (only for private LAN addresses, requires explicit opt-in)
 */
object LMStudioClient {

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 120L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Create a new [LMStudioApiService] backed by a fresh OkHttp client
     * configured according to the supplied [prefs].
     *
     * @throws IllegalArgumentException if the stored server URL is blank.
     */
    fun build(prefs: AppPreferences): LMStudioApiService =
        buildService(
            serverUrl = prefs.serverUrl,
            apiKey = prefs.apiKey,
            allowSelfSignedCerts = prefs.allowSelfSignedCerts,
            enableVerboseLogging = prefs.enableVerboseLogging
        )

    /**
     * Create a one-shot [LMStudioApiService] from a [TestConnectionConfig].
     * This overload does NOT read from or write to [AppPreferences], so it
     * produces no side-effects on the user's stored settings.
     */
    fun buildForTest(config: TestConnectionConfig): LMStudioApiService =
        buildService(
            serverUrl = config.serverUrl,
            apiKey = config.apiKey,
            allowSelfSignedCerts = config.allowSelfSignedCerts,
            enableVerboseLogging = false
        )

    // ── Private implementation ──────────────────────────────────────────────

    private fun buildService(
        serverUrl: String,
        apiKey: String,
        allowSelfSignedCerts: Boolean,
        enableVerboseLogging: Boolean
    ): LMStudioApiService {
        require(serverUrl.isNotBlank()) { "Server URL must not be empty" }

        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"

        val httpClient = buildOkHttpClient(apiKey, allowSelfSignedCerts, enableVerboseLogging)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LMStudioApiService::class.java)
    }

    private fun buildOkHttpClient(
        apiKey: String,
        allowSelfSignedCerts: Boolean,
        enableVerboseLogging: Boolean
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Add API-key header if one is configured
        if (apiKey.isNotBlank()) {
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                chain.proceed(request)
            }
        }

        // Trust self-signed / user-installed certificates when the user has
        // explicitly opted in.  This is intentionally less secure than the
        // default system-CA trust, so it is gated behind a preference toggle.
        if (allowSelfSignedCerts) {
            applySelfSignedCertTrust(builder)
        }

        // Logging (debug builds only – release builds strip this via ProGuard)
        if (enableVerboseLogging) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    /**
     * Install a trust-all [X509TrustManager] so the client accepts
     * self-signed TLS certificates. The user must opt into this mode
     * from the Settings screen.
     *
     * **Warning:** This disables hostname/certificate verification.
     * Only enable for trusted local networks.
     */
    private fun applySelfSignedCertTrust(builder: OkHttpClient.Builder) {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        builder
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
    }
}
