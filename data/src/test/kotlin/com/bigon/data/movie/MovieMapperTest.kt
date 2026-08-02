package com.bigon.data.movie

import com.bigon.core.database.MovieEntity
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The three TMDB quirks this mapper exists to absorb. Each of these would be a
 * user-visible defect if it reached the UI unchanged.
 */
class MovieMapperTest {

    private fun dto(
        releaseDate: String? = "2026-07-28",
        voteAverage: Double = 8.0,
        voteCount: Int = 1200,
        genreIds: List<Int> = listOf(878, 28),
    ) = MovieDto(
        id = 1,
        title = "Spider-Man",
        overview = "…",
        posterPath = "/poster.jpg",
        backdropPath = null,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genreIds = genreIds,
    )

    private fun entityOf(dto: MovieDto) =
        MovieMapper.toEntity(dto, listKey = "popular", page = 1, position = 3)

    @Test
    fun `empty release date becomes null instead of throwing`() {
        val entity = entityOf(dto(releaseDate = ""))
        assertNull(entity.releaseDate)

        val movie = MovieMapper.toDomain(entity, emptyMap())
        assertNull(movie.releaseDate)
        assertNull(movie.releaseYear)
    }

    @Test
    fun `malformed release date degrades to null rather than crashing`() {
        val entity = entityOf(dto(releaseDate = "not-a-date"))
        assertNull(MovieMapper.toDomain(entity, emptyMap()).releaseDate)
    }

    @Test
    fun `valid release date survives the round trip`() {
        val movie = MovieMapper.toDomain(entityOf(dto()), emptyMap())
        assertEquals(LocalDate.of(2026, 7, 28), movie.releaseDate)
        assertEquals(2026, movie.releaseYear)
    }

    @Test
    fun `unrated title carries a null rating, not TMDB's zero`() {
        val entity = entityOf(dto(voteAverage = 0.0, voteCount = 0))
        assertNull(entity.voteAverage)
        assertNull(MovieMapper.toDomain(entity, emptyMap()).voteAverage)
    }

    @Test
    fun `rated title keeps its score`() {
        assertEquals(8.0, MovieMapper.toDomain(entityOf(dto()), emptyMap()).voteAverage)
    }

    @Test
    fun `genre ids resolve to names, unknown ids are dropped`() {
        val entity = entityOf(dto(genreIds = listOf(878, 28, 9999)))
        val movie = MovieMapper.toDomain(entity, mapOf(878 to "Science Fiction", 28 to "Action"))
        assertEquals(listOf("Science Fiction", "Action"), movie.genres)
    }

    @Test
    fun `genres are empty when the genre table has not been fetched`() {
        assertEquals(emptyList(), MovieMapper.toDomain(entityOf(dto()), emptyMap()).genres)
    }

    @Test
    fun `poster path becomes a loadable url and null stays null`() {
        val withPoster = MovieMapper.toDomain(entityOf(dto()), emptyMap())
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", withPoster.posterUrl)
        assertNull(withPoster.backdropUrl)
    }

    @Test
    fun `list key and server ordering are preserved`() {
        val entity: MovieEntity = entityOf(dto())
        assertEquals("popular", entity.listKey)
        assertEquals(3, entity.position)
    }

    // ── detail mapping ──────────────────────────────────────────────────────

    @Test
    fun `detail prefers the official youtube trailer`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                videos = VideosDto(
                    results = listOf(
                        VideoDto(key = "unofficial", site = "YouTube", type = "Trailer", official = false),
                        VideoDto(key = "official", site = "YouTube", type = "Trailer", official = true),
                    ),
                ),
            ),
        )
        assertEquals("official", detail.trailerKey)
    }

    @Test
    fun `detail ignores non-youtube and non-trailer videos`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                videos = VideosDto(
                    results = listOf(
                        VideoDto(key = "vimeo", site = "Vimeo", type = "Trailer", official = true),
                        VideoDto(key = "featurette", site = "YouTube", type = "Featurette", official = true),
                    ),
                ),
            ),
        )
        assertNull(detail.trailerKey)
    }

    @Test
    fun `detail keeps billing order and caps the cast list`() {
        val cast = (1..20).map { CastDto(id = it.toLong(), name = "Actor $it", order = 20 - it) }
        val detail = MovieMapper.toDetail(MovieDetailResponse(id = 1, credits = CreditsDto(cast = cast)))

        assertEquals(12, detail.cast.size)
        assertEquals("Actor 20", detail.cast.first().name) // lowest `order` bills first
    }

    @Test
    fun `detail normalises zero runtime and blank tagline to null`() {
        val detail = MovieMapper.toDetail(MovieDetailResponse(id = 1, runtime = 0, tagline = "   "))
        assertNull(detail.runtimeMinutes)
        assertNull(detail.tagline)
    }

    @Test
    fun `detail applies the same unrated rule as the list mapper`() {
        val unrated = MovieMapper.toDetail(MovieDetailResponse(id = 1, voteAverage = 0.0, voteCount = 0))
        assertNull(unrated.voteAverage)
    }

    @Test
    fun `direct dto mapping applies the same normalisation as the cached path`() {
        val movie = MovieMapper.toDomain(
            dto(releaseDate = "", voteAverage = 0.0, voteCount = 0),
            mapOf(878 to "Science Fiction"),
        )
        assertNull(movie.releaseDate)
        assertNull(movie.voteAverage)
        assertEquals(listOf("Science Fiction"), movie.genres)
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", movie.posterUrl)
    }
}
