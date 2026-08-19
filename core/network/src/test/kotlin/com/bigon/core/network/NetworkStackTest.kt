package com.bigon.core.network

import com.bigon.core.common.AppError
import com.bigon.core.common.AppResult
import com.bigon.core.network.error.NetworkErrorMapper
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Exercises the real Retrofit + OkHttp stack against a local MockWebServer:
 * conversion, auth, and the failure→AppError contract.
 */
class NetworkStackTest {

    @Serializable
    private data class TestDto(val id: Long, val title: String)

    private interface TestApi {
        @GET("movie/{id}")
        suspend fun movie(@Path("id") id: Long): TestDto
    }

    private lateinit var server: MockWebServer
    private lateinit var api: TestApi
    private lateinit var apiCaller: ApiCaller

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val config = NetworkConfig(
            baseUrl = server.url("/").toString(),
            auth = AuthScheme.BearerToken("test-token"),
        )
        val okHttpClient = OkHttpClientFactory(config).create()
        val retrofit = RetrofitFactory(config, okHttpClient, Json { ignoreUnknownKeys = true }).create()

        api = retrofit.create(TestApi::class.java)
        apiCaller = ApiCaller(NetworkErrorMapper())
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `parses successful response into typed result`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"id": 42, "title": "Heat", "extra_field": true}""")
                .build(),
        )

        val result = apiCaller.execute { api.movie(42) }

        assertEquals(AppResult.Success(TestDto(id = 42, title = "Heat")), result)
    }

    @Test
    fun `maps http failure to AppError Http with status code`() = runTest {
        server.enqueue(MockResponse.Builder().code(404).body("not found").build())

        val result = apiCaller.execute { api.movie(9999) }

        val failure = assertIs<AppResult.Failure>(result)
        val error = assertIs<AppError.Network.Http>(failure.error)
        assertEquals(404, error.code)
    }

    @Test
    fun `maps malformed body to serialization error`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("""{"unexpected": []}""").build())

        val result = apiCaller.execute { api.movie(42) }

        val failure = assertIs<AppResult.Failure>(result)
        assertIs<AppError.Serialization>(failure.error)
    }

    @Test
    fun `attaches bearer token and resolves against base url`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(200).body("""{"id": 1, "title": "ok"}""").build(),
        )

        apiCaller.execute { api.movie(1) }

        val recorded = server.takeRequest()
        assertEquals("Bearer test-token", recorded.headers["Authorization"])
        assertEquals("/movie/1", recorded.target)
    }

    // ── credential schemes ──────────────────────────────────────────────────

    private fun apiFor(auth: AuthScheme): TestApi {
        val config = NetworkConfig(
            baseUrl = server.url("/").toString(),
            auth = auth,
        )
        return RetrofitFactory(config, OkHttpClientFactory(config).create(), Json)
            .create()
            .create(TestApi::class.java)
    }

    private fun enqueueOk() = server.enqueue(
        MockResponse.Builder().code(200).body("""{"id": 1, "title": "ok"}""").build(),
    )

    @Test
    fun `a query-param credential travels in the URL, not a header`() = runTest {
        enqueueOk()

        apiCaller.execute { apiFor(AuthScheme.QueryParam("api_key", "v3-key")).movie(1) }

        val recorded = server.takeRequest()
        assertEquals("/movie/1?api_key=v3-key", recorded.target)
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun `a bearer credential travels as a header, leaving the URL clean`() = runTest {
        enqueueOk()

        apiCaller.execute { apiFor(AuthScheme.BearerToken("v4-token")).movie(1) }

        val recorded = server.takeRequest()
        assertEquals("Bearer v4-token", recorded.headers["Authorization"])
        assertEquals("/movie/1", recorded.target)
    }

    @Test
    fun `no credential sends an unauthenticated request rather than failing locally`() = runTest {
        enqueueOk()

        apiCaller.execute { apiFor(AuthScheme.None).movie(1) }

        val recorded = server.takeRequest()
        assertEquals("/movie/1", recorded.target)
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun `a scheme never exposes its secret in toString`() {
        assertFalse(AuthScheme.BearerToken("v4-token").toString().contains("v4-token"))
        assertFalse(AuthScheme.QueryParam("api_key", "v3-key").toString().contains("v3-key"))
    }
}
