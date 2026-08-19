package com.bigon.tmdb.data

import com.bigon.core.network.AuthScheme

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
 * root, so no module below the app knows how it is credentialed.
 */
data class TmdbCredentials(
    val readAccessToken: String = "",
    val apiKey: String = "",
) {
    /**
     * Resolves to the shape :core:network understands. Which of TMDB's two
     * credentials this is stays here, in the module that knows what TMDB is;
     * the network layer receives only "header" or "query parameter".
     */
    val authScheme: AuthScheme = when {
        readAccessToken.isNotBlank() -> AuthScheme.BearerToken(readAccessToken)
        apiKey.isNotBlank() -> AuthScheme.QueryParam(API_KEY_QUERY_PARAM, apiKey)
        else -> AuthScheme.None
    }

    /** Never let a credential reach a log or a crash report by accident. */
    override fun toString(): String = "TmdbCredentials(scheme=${authScheme::class.simpleName})"

    companion object {
        /** Query parameter TMDB expects for the v3 key — also the name to redact in logs. */
        const val API_KEY_QUERY_PARAM = "api_key"
    }
}
