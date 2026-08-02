package com.bigon.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the [Retrofit] instance every API service is created from.
 * Service interfaces themselves live beside their DTOs in :data — Retrofit is
 * interface-driven, so each feature owns its own endpoint contract.
 */
class RetrofitFactory(
    private val config: NetworkConfig,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {

    fun create(): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
