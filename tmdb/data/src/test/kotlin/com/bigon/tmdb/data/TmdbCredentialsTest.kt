package com.bigon.tmdb.data

import com.bigon.core.network.AuthScheme
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import org.junit.Test

/**
 * Which of TMDB's two credentials wins is a TMDB rule, so it is tested here
 * rather than in :core:network — that module now knows only "header" or "query
 * parameter" and could not express this question.
 */
class TmdbCredentialsTest {

    @Test
    fun `the v4 token wins when both are configured`() {
        // The v3 key can only travel in the URL, so preferring it would put a
        // secret in every logged request for no benefit.
        val both = TmdbCredentials(readAccessToken = "v4-token", apiKey = "v3-key")

        val scheme = assertIs<AuthScheme.BearerToken>(both.authScheme)
        assertEquals("v4-token", scheme.token)
    }

    @Test
    fun `the v3 key is used when it is all there is`() {
        val scheme = assertIs<AuthScheme.QueryParam>(TmdbCredentials(apiKey = "v3-key").authScheme)

        assertEquals("api_key", scheme.name)
        assertEquals("v3-key", scheme.value)
    }

    @Test
    fun `no credential resolves to unauthenticated rather than throwing`() {
        // So a misconfigured build surfaces TMDB's own 401 instead of failing
        // locally in a way that looks like a bug in the app.
        assertEquals(AuthScheme.None, TmdbCredentials().authScheme)
    }

    @Test
    fun `credentials never expose their values in toString`() {
        val rendered = TmdbCredentials(readAccessToken = "v4-token", apiKey = "v3-key").toString()

        assertFalse(rendered.contains("v4-token"))
        assertFalse(rendered.contains("v3-key"))
    }
}
