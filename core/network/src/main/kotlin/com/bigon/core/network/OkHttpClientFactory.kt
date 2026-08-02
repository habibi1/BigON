package com.bigon.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Builds the shared [OkHttpClient]: timeouts, auth, and — in debug builds only —
 * wire logging. The log sink is injected ([HttpLoggingInterceptor.Logger]) so
 * this module stays free of any Android logging framework; :app supplies a
 * Timber-backed implementation.
 */
class OkHttpClientFactory(
    private val config: NetworkConfig,
    private val logger: HttpLoggingInterceptor.Logger? = null,
) {

    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(config.socketTimeoutMillis, TimeUnit.MILLISECONDS)
        .callTimeout(config.requestTimeoutMillis, TimeUnit.MILLISECONDS)
        .addInterceptor(AuthInterceptor(config.credentials, apiHost = config.apiHost()))
        .apply {
            if (config.enableLogging && logger != null) {
                addInterceptor(
                    HttpLoggingInterceptor(logger).apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        // Credentials must never reach logcat, in either scheme:
                        // the v4 token rides a header, the v3 key rides the URL.
                        redactHeader("Authorization")
                        redactQueryParams(TmdbCredentials.API_KEY_QUERY_PARAM)
                    },
                )
            }
        }
        .build()
}

/** Host that credentials may be attached to, derived from the configured base URL. */
private fun NetworkConfig.apiHost(): String = baseUrl.toHttpUrlOrNull()?.host.orEmpty()
