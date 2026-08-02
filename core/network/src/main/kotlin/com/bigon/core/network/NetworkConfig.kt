package com.bigon.core.network

/**
 * Injected from the composition root (:app) — the portable network module never
 * reads BuildConfig or platform state itself.
 */
data class NetworkConfig(
    val baseUrl: String,
    val credentials: TmdbCredentials,
    val connectTimeoutMillis: Long = 10_000,
    val requestTimeoutMillis: Long = 30_000,
    val socketTimeoutMillis: Long = 30_000,
    val enableLogging: Boolean = false,
)
