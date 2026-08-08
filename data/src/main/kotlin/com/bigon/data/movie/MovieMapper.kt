package com.bigon.data.movie

import com.bigon.core.database.GenreEntity
import com.bigon.core.database.MovieEntity
import com.bigon.core.database.TrendingItemEntity
import com.bigon.core.model.CastMember
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieDetail
import com.bigon.core.model.MoviePage
import com.bigon.core.model.Review
import com.bigon.core.model.ReviewPage
import com.bigon.core.model.CollectionRef
import com.bigon.core.model.Credit
import com.bigon.core.model.MovieCollection
import com.bigon.core.model.PersonDetail
import com.bigon.core.model.TrendingItem
import com.bigon.core.model.TvDetail
import com.bigon.core.model.WatchProvider
import com.bigon.core.model.WatchProviders
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Translates between TMDB's wire shape, the Room row, and the domain entity.
 *
 * This is where TMDB's three sharp edges are filed off, once, so nothing
 * downstream has to remember them:
 *  - `release_date` is `""` rather than null for unknown dates, which would
 *    throw on parse;
 *  - `vote_average` is `0.0` rather than null for unrated titles, which would
 *    otherwise render as a real "★ 0.0" badge;
 *  - genres arrive as ids and must be resolved against the cached genre table.
 */
internal object MovieMapper {

    fun toEntity(dto: MovieDto, listKey: String, page: Int, position: Int): MovieEntity = MovieEntity(
        id = dto.id,
        listKey = listKey,
        page = page,
        position = position,
        title = dto.title,
        overview = dto.overview,
        posterPath = dto.posterPath,
        backdropPath = dto.backdropPath,
        releaseDate = dto.releaseDate?.takeIf { it.isNotBlank() },
        voteAverage = dto.voteAverage.takeIf { dto.voteCount > 0 && it > 0.0 },
        genreIds = dto.genreIds,
    )

    fun toDomain(entity: MovieEntity, genresById: Map<Int, String>): Movie = Movie(
        id = entity.id,
        title = entity.title,
        overview = entity.overview,
        posterUrl = TmdbImageUrl.build(entity.posterPath, TmdbImageUrl.POSTER_CARD),
        backdropUrl = TmdbImageUrl.build(entity.backdropPath, TmdbImageUrl.BACKDROP_WIDE),
        releaseDate = entity.releaseDate.toLocalDateOrNull(),
        voteAverage = entity.voteAverage,
        genres = entity.genreIds.mapNotNull(genresById::get),
    )

    fun toGenreEntity(dto: GenreDto): GenreEntity = GenreEntity(id = dto.id, name = dto.name)

    /**
     * Search/discover results go straight to the domain without a Room row —
     * they are transient and must not pollute the category cache. Routing
     * through [toEntity] keeps every normalisation rule in one place.
     */
    fun toDomain(dto: MovieDto, genresById: Map<Int, String>): Movie =
        toDomain(toEntity(dto, listKey = TRANSIENT_LIST_KEY, page = 1, position = 0), genresById)

    fun toPage(response: MovieListResponse, genresById: Map<Int, String>): MoviePage = MoviePage(
        movies = response.results.map { toDomain(it, genresById) },
        page = response.page,
        totalPages = response.totalPages,
    )

    private const val TRANSIENT_LIST_KEY = ""

    /**
     * Region used for certification and streaming availability when the device
     * locale has no entry. US is the fallback because TMDB's coverage there is
     * the most complete — not because it is a sensible default for a user.
     */
    const val FALLBACK_REGION = "US"

    /** TMDB's default copy is English, so that is what "not translated" means. */
    const val FALLBACK_LANGUAGE = "en"

    /** Theatrical release; the certification type users recognise. */
    private const val TYPE_THEATRICAL = 3

    fun toDetail(
        dto: MovieDetailResponse,
        genresById: Map<Int, String> = emptyMap(),
        region: String = FALLBACK_REGION,
        language: String = FALLBACK_LANGUAGE,
    ): MovieDetail = toDetailInternal(dto, genresById, region, language)

    private fun toDetailInternal(
        dto: MovieDetailResponse,
        genresById: Map<Int, String>,
        region: String,
        language: String,
    ): MovieDetail = baseDetail(dto).localised(dto, language).copy(
        // Recommendations are editorially better but thin for obscure titles;
        // `similar` is genre-derived and almost always populated. Merging with
        // recommendations first keeps the good ones on top and still fills the
        // row when TMDB has nothing curated.
        recommendations = (dto.recommendations?.results.orEmpty() + dto.similar?.results.orEmpty())
            .distinctBy { it.id }
            .filter { it.id != dto.id }
            .take(MAX_RECOMMENDATIONS)
            .map { toDomain(it, genresById) },
        certification = dto.releaseDates.certificationFor(region),
        keywords = dto.keywords.keywords.map { it.name }.filter { it.isNotBlank() }.take(MAX_KEYWORDS),
        imdbId = dto.externalIds.imdbId?.takeIf { it.isNotBlank() },
        watchProviders = dto.watchProviders.forRegion(region),
        logoUrl = dto.images.bestLogo(),
        alternativeTitles = dto.alternativeTitles.titles
            .map { it.title.trim() }
            .filter { it.isNotBlank() && !it.equals(dto.title, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .take(MAX_ALTERNATIVE_TITLES),
        collection = dto.belongsToCollection
            ?.takeIf { it.id != 0L && it.name.isNotBlank() }
            ?.let { CollectionRef(id = it.id, name = it.name) },
    )

    /**
     * Swaps in TMDB's translation for [language] when one exists.
     *
     * Deliberately field-by-field rather than all-or-nothing: translations are
     * community-contributed and frequently partial, so a record with a
     * translated overview and an empty tagline should yield the translated
     * overview and the English tagline, not force a choice between them.
     *
     * The app's own UI is not localised, so this is the one place a user in a
     * non-English locale sees their language. [MovieDetail.isLocalised] records
     * that it happened rather than letting it look like full localisation.
     */
    private fun MovieDetail.localised(dto: MovieDetailResponse, language: String): MovieDetail {
        if (language.equals(FALLBACK_LANGUAGE, ignoreCase = true)) return this

        val translation = dto.translations.translations
            .firstOrNull { it.language.equals(language, ignoreCase = true) }
            ?.data
            ?: return this

        val localisedOverview = translation.overview.takeIf { it.isNotBlank() }
        val localisedTagline = translation.tagline.takeIf { it.isNotBlank() }
        if (localisedOverview == null && localisedTagline == null) return this

        return copy(
            overview = localisedOverview ?: overview,
            tagline = localisedTagline ?: tagline,
            isLocalised = true,
        )
    }

    /**
     * The widest well-rated English logo. Width is the tiebreak because these
     * are rendered across the backdrop, where an undersized asset is the
     * visible failure; TMDB's vote average filters out the poor scans.
     */
    private fun ImagesDto.bestLogo(): String? = logos
        .filter { it.filePath.isNotBlank() }
        .maxWithOrNull(compareBy({ it.voteAverage }, { it.width }))
        ?.let { TmdbImageUrl.build(it.filePath, TmdbImageUrl.LOGO_WIDE) }

    private const val MAX_ALTERNATIVE_TITLES = 6

    /**
     * Certification for [region], falling back to [FALLBACK_REGION]. TMDB
     * returns several entries per country (theatrical, digital, physical);
     * theatrical is preferred, then whatever is present, and blanks are
     * common enough to filter explicitly.
     */
    private fun ReleaseDatesDto.certificationFor(region: String): String? {
        fun certOf(country: String): String? = results
            .firstOrNull { it.country.equals(country, ignoreCase = true) }
            ?.releases
            ?.let { releases ->
                releases.firstOrNull { it.type == TYPE_THEATRICAL && it.certification.isNotBlank() }
                    ?: releases.firstOrNull { it.certification.isNotBlank() }
            }
            ?.certification
            ?.takeIf { it.isNotBlank() }

        return certOf(region) ?: certOf(FALLBACK_REGION)
    }

    private fun WatchProvidersDto.forRegion(region: String): WatchProviders? {
        val country = results[region.uppercase()] ?: results[FALLBACK_REGION] ?: return null
        val resolved = if (results.containsKey(region.uppercase())) region.uppercase() else FALLBACK_REGION
        return WatchProviders(
            region = resolved,
            link = country.link,
            streaming = country.flatrate.toProviders(),
            rent = country.rent.toProviders(),
            buy = country.buy.toProviders(),
        ).takeIf { !it.isEmpty }
    }

    private fun List<ProviderDto>.toProviders(): List<WatchProvider> = this
        .sortedBy { it.displayPriority }
        .map {
            WatchProvider(
                id = it.providerId,
                name = it.providerName,
                logoUrl = TmdbImageUrl.build(it.logoPath, TmdbImageUrl.LOGO_SMALL),
            )
        }

    private const val MAX_RECOMMENDATIONS = 20
    private const val MAX_KEYWORDS = 8

    private fun baseDetail(dto: MovieDetailResponse): MovieDetail = MovieDetail(
        id = dto.id,
        title = dto.title,
        tagline = dto.tagline?.takeIf { it.isNotBlank() },
        overview = dto.overview,
        posterUrl = TmdbImageUrl.build(dto.posterPath, TmdbImageUrl.POSTER_CARD),
        backdropUrl = TmdbImageUrl.build(dto.backdropPath, TmdbImageUrl.BACKDROP_WIDE),
        releaseDate = dto.releaseDate.toLocalDateOrNull(),
        voteAverage = dto.voteAverage.takeIf { dto.voteCount > 0 && it > 0.0 },
        voteCount = dto.voteCount,
        runtimeMinutes = dto.runtime?.takeIf { it > 0 },
        genres = dto.genres.map { it.name },
        cast = dto.credits.cast
            .sortedBy { it.order }
            .take(MAX_CAST)
            .map { cast ->
                CastMember(
                    id = cast.id,
                    name = cast.name,
                    character = cast.character,
                    profileUrl = TmdbImageUrl.build(cast.profilePath, TmdbImageUrl.PROFILE_SMALL),
                )
            },
        // Prefer an official YouTube trailer; fall back to any YouTube trailer.
        trailerKey = dto.videos.results
            .filter { it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) }
            .minByOrNull { if (it.official) 0 else 1 }
            ?.key
            ?.takeIf { it.isNotBlank() },
    )

    private const val MAX_CAST = 12

    fun toReviewPage(response: ReviewListResponse): ReviewPage = ReviewPage(
        reviews = response.results.map { dto ->
            Review(
                id = dto.id,
                author = dto.author.takeIf { it.isNotBlank() } ?: dto.authorDetails.username,
                content = dto.content.trim(),
                // TMDB avatar paths are sometimes a full Gravatar URL smuggled
                // in behind a leading slash; passing those through the CDN
                // builder would produce a 404.
                avatarUrl = dto.authorDetails.avatarPath?.let { path ->
                    val cleaned = path.removePrefix("/")
                    if (cleaned.startsWith("http")) cleaned
                    else TmdbImageUrl.build(path, TmdbImageUrl.AVATAR_SMALL)
                },
                rating = dto.authorDetails.rating?.takeIf { it > 0.0 },
                createdAt = dto.createdAt?.substringBefore("T").toLocalDateOrNull(),
            )
        }.filter { it.content.isNotBlank() },
        page = response.page,
        totalPages = response.totalPages,
        totalResults = response.totalResults,
    )

    // ── collections, people, series ─────────────────────────────────────────

    fun toCollection(dto: CollectionResponse, genresById: Map<Int, String>): MovieCollection =
        MovieCollection(
            id = dto.id,
            name = dto.name,
            overview = dto.overview,
            posterUrl = TmdbImageUrl.build(dto.posterPath, TmdbImageUrl.POSTER_CARD),
            backdropUrl = TmdbImageUrl.build(dto.backdropPath, TmdbImageUrl.BACKDROP_WIDE),
            // Parts arrive in release order only by accident; sorting makes the
            // franchise readable as a sequence, which is why people open it.
            parts = dto.parts
                .sortedBy { it.releaseDate?.takeIf { d -> d.isNotBlank() } ?: "9999" }
                .map { toDomain(it, genresById) },
        )

    /**
     * Cast and crew credits merged into one filmography.
     *
     * Users think in "things this person made", not TMDB's department split, so
     * a director who also acted appears once per film rather than in two
     * disconnected lists. Ordered newest first because recent work is what a
     * person is currently known for; undated entries (announced but unscheduled)
     * sort last rather than jumping to the top.
     */
    fun toPersonDetail(dto: PersonResponse, genresById: Map<Int, String>): PersonDetail {
        val cast = dto.movieCredits.cast.map { it.id to Credit(
            movie = personCredit(it.id, it.title, it.overview, it.posterPath, it.backdropPath,
                it.releaseDate, it.voteAverage, it.voteCount, it.genreIds, genresById),
            role = it.character.takeIf { c -> c.isNotBlank() },
        ) }
        val crew = dto.movieCredits.crew.map { it.id to Credit(
            movie = personCredit(it.id, it.title, it.overview, it.posterPath, it.backdropPath,
                it.releaseDate, it.voteAverage, it.voteCount, it.genreIds, genresById),
            role = it.job.takeIf { j -> j.isNotBlank() },
        ) }

        return PersonDetail(
            id = dto.id,
            name = dto.name,
            biography = dto.biography.trim(),
            profileUrl = TmdbImageUrl.build(dto.profilePath, TmdbImageUrl.PROFILE_LARGE),
            knownForDepartment = dto.knownForDepartment?.takeIf { it.isNotBlank() },
            birthday = dto.birthday.toLocalDateOrNull(),
            deathday = dto.deathday.toLocalDateOrNull(),
            placeOfBirth = dto.placeOfBirth?.takeIf { it.isNotBlank() },
            filmography = (cast + crew)
                .distinctBy { it.first }
                .map { it.second }
                .filter { it.movie.title.isNotBlank() }
                .sortedByDescending { it.movie.releaseDate?.toString() ?: "" }
                .take(MAX_FILMOGRAPHY),
        )
    }

    @Suppress("LongParameterList")
    private fun personCredit(
        id: Long,
        title: String,
        overview: String,
        posterPath: String?,
        backdropPath: String?,
        releaseDate: String?,
        voteAverage: Double,
        voteCount: Int,
        genreIds: List<Int>,
        genresById: Map<Int, String>,
    ): Movie = Movie(
        id = id,
        title = title,
        overview = overview,
        posterUrl = TmdbImageUrl.build(posterPath, TmdbImageUrl.POSTER_CARD),
        backdropUrl = TmdbImageUrl.build(backdropPath, TmdbImageUrl.BACKDROP_WIDE),
        releaseDate = releaseDate.toLocalDateOrNull(),
        // The unrated rule from the list mapper applies to credits too.
        voteAverage = voteAverage.takeIf { voteCount > 0 && it > 0.0 },
        genres = genreIds.mapNotNull(genresById::get),
    )

    fun toTvDetail(dto: TvDetailResponse, region: String = FALLBACK_REGION): TvDetail = TvDetail(
        id = dto.id,
        name = dto.name,
        tagline = dto.tagline?.takeIf { it.isNotBlank() },
        overview = dto.overview,
        posterUrl = TmdbImageUrl.build(dto.posterPath, TmdbImageUrl.POSTER_CARD),
        backdropUrl = TmdbImageUrl.build(dto.backdropPath, TmdbImageUrl.BACKDROP_WIDE),
        firstAirDate = dto.firstAirDate.toLocalDateOrNull(),
        lastAirDate = dto.lastAirDate.toLocalDateOrNull(),
        voteAverage = dto.voteAverage.takeIf { dto.voteCount > 0 && it > 0.0 },
        voteCount = dto.voteCount,
        numberOfSeasons = dto.numberOfSeasons,
        numberOfEpisodes = dto.numberOfEpisodes,
        status = dto.status?.takeIf { it.isNotBlank() },
        genres = dto.genres.map { it.name },
        cast = dto.credits.cast
            .sortedBy { it.order }
            .take(MAX_CAST)
            .map { CastMember(it.id, it.name, it.character, TmdbImageUrl.build(it.profilePath, TmdbImageUrl.PROFILE_SMALL)) },
        trailerKey = dto.videos.results
            .filter { it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true) }
            .minByOrNull { if (it.official) 0 else 1 }
            ?.key
            ?.takeIf { it.isNotBlank() },
        // Flatter than a film's release_dates: one rating per country, no
        // release-type nesting to pick through.
        certification = dto.contentRatings.results
            .firstOrNull { it.country.equals(region, ignoreCase = true) && it.rating.isNotBlank() }
            ?.rating
            ?: dto.contentRatings.results
                .firstOrNull { it.country.equals(FALLBACK_REGION, ignoreCase = true) && it.rating.isNotBlank() }
                ?.rating,
        watchProviders = dto.watchProviders.forRegion(region),
    )

    private const val MAX_FILMOGRAPHY = 40

    // ── mixed trending feed ─────────────────────────────────────────────────

    const val MEDIA_MOVIE = "movie"
    const val MEDIA_TV = "tv"
    const val MEDIA_PERSON = "person"

    /**
     * Rows for the cache, keeping TMDB's ranking as [TrendingItemEntity.position].
     *
     * Entries whose `media_type` is none of the three known values are dropped
     * rather than coerced. TMDB has been observed returning types its own docs
     * do not list, and a feed that renders an unknown thing as an empty card is
     * worse than one that is briefly shorter.
     */
    fun toTrendingEntities(response: TrendingListResponse): List<TrendingItemEntity> =
        response.results
            .filter { it.mediaType in setOf(MEDIA_MOVIE, MEDIA_TV, MEDIA_PERSON) && it.id != 0L }
            .mapIndexed { index, dto ->
                TrendingItemEntity(
                    id = dto.id,
                    mediaType = dto.mediaType,
                    position = index,
                    title = dto.title ?: dto.name.orEmpty(),
                    overview = dto.overview,
                    imagePath = dto.posterPath ?: dto.profilePath,
                    datedOn = (dto.releaseDate ?: dto.firstAirDate)?.takeIf { it.isNotBlank() },
                    // The unrated rule from the list mapper applies here too.
                    voteAverage = dto.voteAverage.takeIf { dto.voteCount > 0 && it > 0.0 },
                    genreIds = dto.genreIds,
                    knownForDepartment = dto.knownForDepartment,
                    knownFor = dto.knownFor.mapNotNull { it.title ?: it.name }.filter { it.isNotBlank() },
                )
            }

    fun toTrendingItem(entity: TrendingItemEntity, genresById: Map<Int, String>): TrendingItem? =
        when (entity.mediaType) {
            MEDIA_MOVIE -> TrendingItem.Film(
                Movie(
                    id = entity.id,
                    title = entity.title,
                    overview = entity.overview,
                    posterUrl = TmdbImageUrl.build(entity.imagePath, TmdbImageUrl.POSTER_CARD),
                    backdropUrl = null,
                    releaseDate = entity.datedOn.toLocalDateOrNull(),
                    voteAverage = entity.voteAverage,
                    genres = entity.genreIds.mapNotNull(genresById::get),
                ),
            )

            MEDIA_TV -> TrendingItem.Series(
                id = entity.id,
                name = entity.title,
                overview = entity.overview,
                posterUrl = TmdbImageUrl.build(entity.imagePath, TmdbImageUrl.POSTER_CARD),
                firstAirDate = entity.datedOn.toLocalDateOrNull(),
                voteAverage = entity.voteAverage,
                // TV genre ids come from a different TMDB list than the movie
                // one this app caches, so only overlapping ids resolve. Showing
                // the few that match beats showing a wrong name.
                genres = entity.genreIds.mapNotNull(genresById::get),
            )

            MEDIA_PERSON -> TrendingItem.Person(
                id = entity.id,
                name = entity.title,
                profileUrl = TmdbImageUrl.build(entity.imagePath, TmdbImageUrl.PROFILE_SMALL),
                knownForDepartment = entity.knownForDepartment,
                knownFor = entity.knownFor,
            )

            else -> null
        }

    /** TMDB dates are ISO, but may be absent, blank, or occasionally malformed. */
    private fun String?.toLocalDateOrNull(): LocalDate? =
        this?.takeIf { it.isNotBlank() }?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }
}
