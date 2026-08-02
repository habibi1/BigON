package com.bigon.data.movie

import com.bigon.core.database.GenreEntity
import com.bigon.core.database.MovieEntity
import com.bigon.core.model.CastMember
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieDetail
import com.bigon.core.model.MoviePage
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

    fun toDetail(dto: MovieDetailResponse): MovieDetail = MovieDetail(
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
