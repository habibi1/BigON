package com.bigon.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches TMDB credentials to every request, using whichever scheme is
 * configured (see [TmdbCredentials]). Centralised here so no service interface,
 * repository or call site ever handles a credential — and so switching from the
 * v3 key to the v4 token is a configuration change, not a code change.
 */
class AuthInterceptor(
    private val credentials: TmdbCredentials,
    private val apiHost: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Credentials belong to the API host only. Image requests go to a
        // different TMDB host through the same client, and must never carry
        // them — a token in an image URL would leak into CDN logs.
        if (!request.url.host.equals(apiHost, ignoreCase = true)) {
            return chain.proceed(request)
        }

        val authenticated = when (credentials.scheme) {
            TmdbCredentials.Scheme.BearerToken ->
                request.newBuilder()
                    .header("Authorization", "Bearer ${credentials.readAccessToken}")
                    .build()

            TmdbCredentials.Scheme.ApiKeyQueryParam ->
                request.newBuilder()
                    .url(
                        request.url.newBuilder()
                            .setQueryParameter(
                                TmdbCredentials.API_KEY_QUERY_PARAM,
                                credentials.apiKey,
                            )
                            .build(),
                    )
                    .build()

            // Unauthenticated: let the request through so the 401 surfaces as a
            // normal AppError.Network.Http rather than a silent local failure.
            TmdbCredentials.Scheme.None -> request
        }

        return chain.proceed(authenticated)
    }
}
