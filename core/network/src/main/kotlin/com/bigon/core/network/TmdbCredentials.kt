package com.bigon.core.network

/**
 * TMDB accepts two interchangeable credentials. Both are modelled so the app
 * works with whichever the developer has, but they are not equally safe:
 *
 * - [readAccessToken] (v4, a JWT) travels in an `Authorization` header. Headers
 *   are easy to redact and never end up in URLs, proxy logs, or crash reports.
 *   **Preferred.**
 * - [apiKey] (v3, 32 hex characters) can only travel as an `api_key` query
 *   parameter, so it becomes part of every request URL. Usable, but it must be
 *   redacted explicitly wherever URLs are logged.
 *
 * When both are present the token wins. Neither is ever hardcoded: values come
 * from `local.properties` via `BuildConfig` and are injected by the composition
 * root, so no module below `:sinema` knows how the app is credentialed.
 */
data class TmdbCredentials(
    val readAccessToken: String = "",
    val apiKey: String = "",
) {
    /** The scheme that will actually be used, given what is configured. */
    val scheme: Scheme = when {
        readAccessToken.isNotBlank() -> Scheme.BearerToken
        apiKey.isNotBlank() -> Scheme.ApiKeyQueryParam
        else -> Scheme.None
    }

    enum class Scheme { BearerToken, ApiKeyQueryParam, None }

    /** Never let a credential reach a log or a crash report by accident. */
    override fun toString(): String = "TmdbCredentials(scheme=$scheme)"

    companion object {
        /** Query parameter TMDB expects for the v3 key — also the name to redact in logs. */
        const val API_KEY_QUERY_PARAM = "api_key"
    }
}
