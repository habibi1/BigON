package com.bigon.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the configured credential to every request bound for the API host.
 *
 * Centralised here so no service interface, repository or call site ever handles
 * a credential — and so changing scheme is a configuration change rather than a
 * code change.
 */
class AuthInterceptor(
    private val auth: AuthScheme,
    private val apiHost: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Credentials belong to the API host only. Image requests go to a
        // different host through the same client, and must never carry them —
        // a token in an image URL would leak into CDN logs.
        if (!request.url.host.equals(apiHost, ignoreCase = true)) {
            return chain.proceed(request)
        }

        val authenticated = when (auth) {
            is AuthScheme.BearerToken ->
                request.newBuilder()
                    .header("Authorization", "Bearer ${auth.token}")
                    .build()

            is AuthScheme.QueryParam ->
                request.newBuilder()
                    .url(
                        request.url.newBuilder()
                            .setQueryParameter(auth.name, auth.value)
                            .build(),
                    )
                    .build()

            AuthScheme.None -> request
        }

        return chain.proceed(authenticated)
    }
}
