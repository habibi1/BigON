package com.bigon.tmdb.data.movie

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB endpoint contract. Lives beside its DTOs in :tmdb:data because Retrofit
 * services are wire-shaped, not domain-shaped — each feature owns its own.
 *
 * Every parameter TMDB accepts is declared, not only the ones Sinema currently
 * sends, because this module is meant to be usable by a second movie app that
 * wants different ones. All of them default to `null`, and Retrofit omits a
 * null query parameter entirely — so an unused parameter costs nothing on the
 * wire and changes no existing behaviour.
 *
 * Two of these are absent from TMDB's published reference and verified against
 * the live API instead; both are marked where they appear. The reference is
 * incomplete rather than wrong, so neither source is sufficient alone.
 */
interface MovieApi {

    // ── Trending ────────────────────────────────────────────────────────────

    /**
     * `page` is **not** in TMDB's reference for this endpoint, and works:
     * page 2 returns twenty entirely different films, echoes `"page": 2`, and
     * reports `total_pages: 500`. Verified against the live API — removing it
     * on the strength of the documentation would silently break pagination.
     */
    @GET("trending/movie/day")
    suspend fun trending(
        @Query("page") page: Int = 1,
        @Query("language") language: String? = null,
    ): MovieListResponse

    @GET("trending/movie/week")
    suspend fun trendingWeek(
        @Query("page") page: Int = 1,
        @Query("language") language: String? = null,
    ): MovieListResponse

    /** Films, series and people interleaved, keyed by `media_type`. */
    @GET("trending/all/week")
    suspend fun trendingAll(
        @Query("page") page: Int = 1,
        @Query("language") language: String? = null,
    ): TrendingListResponse

    // ── Curated lists ───────────────────────────────────────────────────────

    /**
     * [region] localises these four lists — `now_playing` in particular becomes
     * actual local cinema listings rather than US ones. Trending takes no
     * region: it is a global signal by definition.
     */
    @GET("movie/popular")
    suspend fun popular(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
    ): MovieListResponse

    @GET("movie/now_playing")
    suspend fun nowPlaying(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
    ): MovieListResponse

    @GET("movie/top_rated")
    suspend fun topRated(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
    ): MovieListResponse

    @GET("movie/upcoming")
    suspend fun upcoming(
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
    ): MovieListResponse

    // ── Search ──────────────────────────────────────────────────────────────

    /**
     * Free-text title search, and the full set TMDB accepts here.
     *
     * Note what is *not* on this list: `with_genres`. TMDB takes the parameter
     * without complaint and returns the identical result set — verified by
     * comparing titles, not just counts — so genre alongside a query has to be
     * applied client-side. [discover] is where genre filtering actually works.
     *
     * [primaryReleaseYear] and [year] *are* accepted, which is easy to miss:
     * "spider" returns 505 results, and 12 once a year is added. So a
     * year filter beside a typed query would work, unlike sort, rating or
     * runtime.
     */
    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("year") year: Int? = null,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
    ): MovieListResponse

    // ── Discover ────────────────────────────────────────────────────────────

    /**
     * Filtered browsing — one endpoint with the widest parameter surface TMDB
     * offers, and the only place genre, provider, rating and runtime filters
     * actually apply.
     *
     * Two pairings TMDB enforces implicitly. [withWatchProviders] must travel
     * with [watchRegion] — a provider filter with no region to resolve it
     * against is silently ignored, which reads as "the filter does nothing"
     * rather than as an error. [minRating] must travel with [minVotes], or a
     * single 10/10 vote outranks a film with a thousand good ones.
     *
     * Sinema uses nine of these. The rest are declared for the same reason the
     * module exists: a second app should not have to edit this file to ask a
     * question TMDB already answers.
     */
    @Suppress("LongParameterList")
    @GET("discover/movie")
    suspend fun discover(
        // — the nine Sinema sends —
        @Query("with_genres") withGenres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("vote_average.gte") minRating: Double? = null,
        @Query("vote_count.gte") minVotes: Int? = null,
        @Query("with_runtime.lte") maxRuntime: Int? = null,

        // — certification —
        @Query("certification") certification: String? = null,
        @Query("certification.gte") certificationGte: String? = null,
        @Query("certification.lte") certificationLte: String? = null,
        @Query("certification_country") certificationCountry: String? = null,

        // — dates —
        @Query("year") year: Int? = null,
        @Query("primary_release_date.gte") primaryReleaseDateGte: String? = null,
        @Query("primary_release_date.lte") primaryReleaseDateLte: String? = null,
        @Query("release_date.gte") releaseDateGte: String? = null,
        @Query("release_date.lte") releaseDateLte: String? = null,
        @Query("with_release_type") withReleaseType: String? = null,

        // — ratings and runtime, the other ends of the ranges —
        @Query("vote_average.lte") maxRating: Double? = null,
        @Query("vote_count.lte") maxVotes: Int? = null,
        @Query("with_runtime.gte") minRuntime: Int? = null,

        // — people and companies —
        @Query("with_cast") withCast: String? = null,
        @Query("with_crew") withCrew: String? = null,
        @Query("with_people") withPeople: String? = null,
        @Query("with_companies") withCompanies: String? = null,
        @Query("without_companies") withoutCompanies: String? = null,

        // — keywords, language, origin —
        @Query("with_keywords") withKeywords: String? = null,
        @Query("without_keywords") withoutKeywords: String? = null,
        @Query("with_origin_country") withOriginCountry: String? = null,
        @Query("with_original_language") withOriginalLanguage: String? = null,
        @Query("without_genres") withoutGenres: String? = null,

        // — availability —
        @Query("with_watch_monetization_types") withWatchMonetizationTypes: String? = null,
        @Query("without_watch_providers") withoutWatchProviders: String? = null,

        // — response shaping —
        @Query("include_adult") includeAdult: Boolean? = null,
        @Query("include_video") includeVideo: Boolean? = null,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
    ): MovieListResponse

    // ── Detail ──────────────────────────────────────────────────────────────

    /**
     * Detail costs one request rather than eight, because TMDB appends related
     * blocks to the same response.
     *
     * The block list is a byte budget, not a wish list. Measured against the
     * live API for one title: `credits,videos` is ~119 KB; these eight blocks
     * are ~224 KB; all Tier 1 blocks would be ~396 KB. `reviews` has its own
     * endpoint below, so it paginates independently and can fail without taking
     * detail with it.
     */
    @GET("movie/{id}")
    suspend fun detail(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = DETAIL_BLOCKS,
        /**
         * Scopes the appended `images` block, and is **not** in TMDB's
         * reference for this endpoint. It works, and does real work: measured
         * on one title, 122.8 KB and 744 images without it, 33.6 KB and 196
         * with it. `en` alone beats `en,null` because `null` is what the
         * textless backdrops are tagged with.
         */
        @Query("include_image_language") imageLanguage: String? = "en",
        @Query("language") language: String? = null,
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
        @Query("language") language: String? = null,
    ): ReviewListResponse

    @GET("genre/movie/list")
    suspend fun genres(
        @Query("language") language: String? = null,
    ): GenreListResponse

    /** A franchise and its parts. Small: parts use the list shape. */
    @GET("collection/{id}")
    suspend fun collection(
        @Path("id") id: Long,
        @Query("language") language: String? = null,
    ): CollectionResponse

    /**
     * A person and their film credits. `movie_credits` is nearly all of the
     * ~92 KB, but it is the point of the screen — a person without their
     * filmography is a photograph and a paragraph.
     */
    @GET("person/{id}")
    suspend fun person(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = "movie_credits",
        @Query("language") language: String? = null,
    ): PersonResponse

    /**
     * Series detail. `content_ratings` rather than `release_dates` — TV's
     * certification lives under a different key with a flatter shape.
     */
    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = TV_BLOCKS,
        @Query("language") language: String? = null,
    ): TvDetailResponse

    // ── Availability ────────────────────────────────────────────────────────

    /** The regions TMDB holds availability data for. */
    @GET("watch/providers/regions")
    suspend fun regions(
        @Query("language") language: String? = null,
    ): RegionListResponse

    /** Services carrying films in [watchRegion], for the browse filter. */
    @GET("watch/providers/movie")
    suspend fun watchProviders(
        @Query("watch_region") watchRegion: String,
        @Query("language") language: String? = null,
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
