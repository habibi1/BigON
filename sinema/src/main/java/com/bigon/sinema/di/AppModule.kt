package com.bigon.sinema.di

import com.bigon.sinema.BuildConfig
import com.bigon.core.common.DefaultDispatcherProvider
import com.bigon.core.common.DispatcherProvider
import com.bigon.core.config.FeatureFlagDataSource
import com.bigon.core.config.StaticFeatureFlagDataSource
import com.bigon.core.network.NetworkConfig
import com.bigon.core.network.OkHttpClientFactory
import com.bigon.core.network.RetrofitFactory
import com.bigon.tmdb.data.TmdbCredentials
import com.bigon.core.tracker.AnalyticsSink
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.tracker.CompositeAnalyticsTracker
import com.bigon.core.tracker.ConsentProvider
import com.bigon.core.tracker.StaticConsentProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Composition root (§3.3): platform bindings and providers for types that live
 * in the framework-free JVM modules. Hilt validates this graph at compile time —
 * a missing binding fails the build, not production.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Concurrency ─────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(dispatchers: DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.default)

    // ── Network ─────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = "https://api.themoviedb.org/3/",
        // Values originate in local.properties; the v4 token wins when present.
        auth = TmdbCredentials(
            readAccessToken = BuildConfig.TMDB_READ_ACCESS_TOKEN,
            apiKey = BuildConfig.TMDB_API_KEY,
        ).authScheme,
        enableLogging = BuildConfig.DEBUG,
    )

    /** Wire logging sink — the only place the network stack meets Timber. */
    @Provides
    @Singleton
    fun provideHttpLogger(): HttpLoggingInterceptor.Logger =
        HttpLoggingInterceptor.Logger { message -> Timber.tag("HttpClient").d(message) }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: NetworkConfig,
        logger: HttpLoggingInterceptor.Logger,
    ): OkHttpClient = OkHttpClientFactory(config, logger).create()

    @Provides
    @Singleton
    fun provideRetrofit(
        config: NetworkConfig,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = RetrofitFactory(config, okHttpClient, json).create()

    // ── Analytics (§6 — sinks are multibound; adding an SDK adds one @IntoSet) ──
    @Provides
    @Singleton
    fun provideConsentProvider(): ConsentProvider = StaticConsentProvider(allowed = true)

    @Provides
    @IntoSet
    @Singleton
    fun provideTimberAnalyticsSink(): AnalyticsSink = TimberAnalyticsSink()

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        sinks: Set<@JvmSuppressWildcards AnalyticsSink>,
        consent: ConsentProvider,
        @ApplicationScope scope: CoroutineScope,
    ): AnalyticsTracker = CompositeAnalyticsTracker(
        sinks = sinks.toList(),
        consent = consent,
        scope = scope,
    )

    // ── Remote config (§7 — no backend yet: reads fall through to defaults) ──
    @Provides
    @Singleton
    fun provideFeatureFlagDataSource(): FeatureFlagDataSource = StaticFeatureFlagDataSource()

}
