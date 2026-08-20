package com.bigon.tmdb.data.movie

/**
 * A [MovieApi] that answers everything with empty data.
 *
 * It exists because the real interface declares every parameter TMDB accepts —
 * `discover` alone takes 38 — and a test that cares about two of them should
 * not have to restate the other thirty-six. Each test subclasses this and
 * overrides only the calls it exercises.
 *
 * The alternative, which this replaces, was three hand-written fakes that each
 * had to be edited whenever a parameter was added. That cost is what makes
 * wide interfaces rot: the interface is easy to widen, and the fakes are what
 * quietly stop compiling.
 */
open class FakeMovieApi : MovieApi {

    override suspend fun trending(page: Int, language: String?) = MovieListResponse()

    override suspend fun trendingWeek(page: Int, language: String?) = MovieListResponse()

    override suspend fun trendingAll(page: Int, language: String?) = TrendingListResponse()

    override suspend fun popular(page: Int, region: String?, language: String?) = MovieListResponse()

    override suspend fun nowPlaying(page: Int, region: String?, language: String?) = MovieListResponse()

    override suspend fun topRated(page: Int, region: String?, language: String?) = MovieListResponse()

    override suspend fun upcoming(page: Int, region: String?, language: String?) = MovieListResponse()

    override suspend fun search(
        query: String,
        page: Int,
        includeAdult: Boolean,
        primaryReleaseYear: Int?,
        year: Int?,
        region: String?,
        language: String?,
    ) = MovieListResponse()

    @Suppress("LongParameterList")
    override suspend fun discover(
        withGenres: String?,
        sortBy: String,
        page: Int,
        withWatchProviders: String?,
        watchRegion: String?,
        primaryReleaseYear: Int?,
        minRating: Double?,
        minVotes: Int?,
        maxRuntime: Int?,
        certification: String?,
        certificationGte: String?,
        certificationLte: String?,
        certificationCountry: String?,
        year: Int?,
        primaryReleaseDateGte: String?,
        primaryReleaseDateLte: String?,
        releaseDateGte: String?,
        releaseDateLte: String?,
        withReleaseType: String?,
        maxRating: Double?,
        maxVotes: Int?,
        minRuntime: Int?,
        withCast: String?,
        withCrew: String?,
        withPeople: String?,
        withCompanies: String?,
        withoutCompanies: String?,
        withKeywords: String?,
        withoutKeywords: String?,
        withOriginCountry: String?,
        withOriginalLanguage: String?,
        withoutGenres: String?,
        withWatchMonetizationTypes: String?,
        withoutWatchProviders: String?,
        includeAdult: Boolean?,
        includeVideo: Boolean?,
        region: String?,
        language: String?,
    ) = MovieListResponse()

    override suspend fun detail(
        id: Long,
        append: String,
        imageLanguage: String?,
        language: String?,
    ) = MovieDetailResponse(id = id)

    override suspend fun reviews(id: Long, page: Int, language: String?) = ReviewListResponse()

    override suspend fun genres(language: String?) = GenreListResponse()

    override suspend fun collection(id: Long, language: String?) = CollectionResponse()

    override suspend fun person(id: Long, append: String, language: String?) = PersonResponse()

    override suspend fun tvDetail(id: Long, append: String, language: String?) = TvDetailResponse()

    override suspend fun regions(language: String?) = RegionListResponse()

    override suspend fun watchProviders(watchRegion: String, language: String?) =
        WatchProviderListResponse()
}
