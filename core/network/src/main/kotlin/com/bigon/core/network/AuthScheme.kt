package com.bigon.core.network

/**
 * How requests to the configured API host are credentialed.
 *
 * The network module deliberately does not know *whose* credential this is. It
 * knows only the two shapes an HTTP credential can take — a header or a query
 * parameter — and which one it was handed. Which API, and where the value came
 * from, are the app's business.
 *
 * The distinction is not cosmetic: a header is easy to redact and never reaches
 * a URL, while a query parameter becomes part of every request URL and has to
 * be redacted explicitly wherever URLs are logged. [redactedQueryParam] exists
 * so the logging interceptor can do that without knowing the scheme.
 */
sealed interface AuthScheme {

    /** The query parameter carrying a secret, if this scheme puts one in the URL. */
    val redactedQueryParam: String? get() = null

    /**
     * Unauthenticated. Requests still go out, so an API's own 401 surfaces as a
     * normal HTTP error rather than a silent local failure that looks like a bug.
     */
    data object None : AuthScheme

    /** `Authorization: Bearer <token>`. Preferred — headers stay out of URLs. */
    data class BearerToken(val token: String) : AuthScheme {
        override fun toString(): String = "BearerToken(***)"
    }

    /** A secret in the query string. Usable, but it lands in every logged URL. */
    data class QueryParam(val name: String, val value: String) : AuthScheme {
        override val redactedQueryParam: String get() = name
        override fun toString(): String = "QueryParam(name=$name, value=***)"
    }
}
