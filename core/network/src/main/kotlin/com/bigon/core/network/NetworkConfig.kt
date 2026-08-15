package com.bigon.core.network

/**
 * Injected from the composition root — the network module never reads
 * BuildConfig or platform state itself, and never knows which API it is
 * calling. [auth] arrives already resolved; see [AuthScheme].
 */
data class NetworkConfig(
    val baseUrl: String,
    val auth: AuthScheme = AuthScheme.None,
    val connectTimeoutMillis: Long = 10_000,
    val requestTimeoutMillis: Long = 30_000,
    val socketTimeoutMillis: Long = 30_000,
    val enableLogging: Boolean = false,
)
