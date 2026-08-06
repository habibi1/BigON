package com.bigon.data.movie

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB endpoint contract. Lives beside its DTOs in :data because Retrofit
 * services are wire-shaped, not domain-shaped — each feature owns its own.
 */
interface MovieApi {

    @GET("trending/movie/day")
    suspend fun trending(@Query("page") page: Int = 1): MovieListResponse

    @GET("trending/movie/week")
    suspend fun trendingWeek(@Query("page") page: Int = 1): MovieListResponse

    /** Films, series and people interleaved, keyed by `media_type`. */
    @GET("trending/all/week")
    suspend fun trendingAll(@Query("page") page: Int = 1): TrendingListResponse

    /**
     * [region] localises these four lists — `now_playing` in particular becomes
     * actual local cinema listings rather than US ones. Trending takes no
     * region: it is a global signal by definition.
     */
    @GET("movie/popular")
    suspend fun popular(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
    ): MovieListResponse

    @GET("movie/now_playing")
    suspend fun nowPlaying(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
    ): MovieListResponse

    @GET("movie/top_rated")
    suspend fun topRated(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
    ): MovieListResponse

    @GET("movie/upcoming")
    suspend fun upcoming(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
    ): MovieListResponse

    /**
     * Free-text title search. TMDB's search endpoint takes no genre filter —
     * genre browsing is [discover]'s job.
     */
    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): MovieListResponse

    /** Filtered browsing; drives the Search tab's idle state and genre chips. */
    /**
     * [withWatchProviders] must travel with [watchRegion] — TMDB silently
     * ignores a provider filter that has no region to resolve it against,
     * which reads as "the filter does nothing" rather than as an error.
     */
    @GET("discover/movie")
    suspend fun discover(
        @Query("with_genres") withGenres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = null,
    ): MovieListResponse

    /**
     * Detail costs one request rather than eight, because TMDB appends related
     * blocks to the same response.
     *
     * The block list is a byte budget, not a wish list. Measured against the
     * live API for one title: `credits,videos` is ~119 KB; these eight blocks
     * are ~224 KB; all Tier 1 blocks would be ~396 KB. Two are deliberately
     * excluded — `images` (132 KB, every language and size) and `reviews`
     * (41 KB for a section most users never open).
     *
     * Both are now covered, differently. `images` is appended but scoped with
     * `include_image_language=en`, which is 31 KB rather than 132 KB. `reviews`
     * has its own endpoint below, so it paginates independently and can fail
     * without taking detail with it.
     */
    @GET("movie/{id}")
    suspend fun detail(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = DETAIL_BLOCKS,
        /**
         * Scopes the appended `images` block. Without it TMDB returns every
         * language and every textless variant — 118 KB rather than 31 KB, for
         * one title logo. `en` alone beats `en,null` because `null` is what
         * the textless backdrops are tagged with.
         */
        @Query("include_image_language") imageLanguage: String = "en",
    ): MovieDetailResponse

    /**
     * Reviews are a separate request rather than an appended block. They are
     * fetched alongside detail, so this is not about deferring the cost — it is
     * about isolation: reviews are paginated on their own axis, and a failure
     * to load them must not take detail's content down with it.
     */
    @GET("movie/{id}/reviews")
    suspend fun reviews(
        @Path("id") id: Long,
        @Query("page") page: Int = 1,
    ): ReviewListResponse

    @GET("genre/movie/list")
    suspend fun genres(): GenreListResponse

    /** A franchise and its parts. Small: parts use the list shape. */
    @GET("collection/{id}")
    suspend fun collection(@Path("id") id: Long): CollectionResponse

    /**
     * A person and their film credits. `movie_credits` is nearly all of the
     * ~92 KB, but it is the point of the screen — a person without their
     * filmography is a photograph and a paragraph.
     */
    @GET("person/{id}")
    suspend fun person(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = "movie_credits",
    ): PersonResponse

    /**
     * Series detail. `content_ratings` rather than `release_dates` — TV's
     * certification lives under a different key with a flatter shape.
     */
    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = TV_BLOCKS,
    ): TvDetailResponse

    /** The regions TMDB holds availability data for. */
    @GET("watch/providers/regions")
    suspend fun regions(): RegionListResponse

    /** Services carrying films in [watchRegion], for the browse filter. */
    @GET("watch/providers/movie")
    suspend fun watchProviders(
        @Query("watch_region") watchRegion: String,
    ): WatchProviderListResponse

    companion object {
        /** Series equivalent of [DETAIL_BLOCKS]; TV has no recommendations worth the bytes. */
        const val TV_BLOCKS: String = "credits,videos,content_ratings,watch/providers"

        /**
         * Appended blocks, cheapest-first. `watch/providers` carries a slash,
         * which is legal in the query value even though it is not a legal
         * Kotlin identifier — see the `@SerialName` on the DTO.
         */
        const val DETAIL_BLOCKS: String =
            "credits,videos,recommendations,similar,release_dates,keywords,external_ids," +
                "watch/providers,images,alternative_titles,translations"
    }
}
