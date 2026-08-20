package com.bigon.tmdb.data.movie

import com.bigon.tmdb.database.MovieEntity
import com.bigon.tmdb.model.TrendingItem
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    // ── appended blocks ─────────────────────────────────────────────────────

    private fun listResponse(vararg ids: Long) = MovieListResponse(
        results = ids.map { MovieDto(id = it, title = "Movie $it") },
    )

    @Test
    fun `recommendations fall back to similar and never repeat the movie itself`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                recommendations = listResponse(2, 3),
                // 3 duplicates a recommendation, 1 is this very movie.
                similar = listResponse(3, 4, 1),
            ),
        )

        assertEquals(listOf(2L, 3L, 4L), detail.recommendations.map { it.id })
    }

    @Test
    fun `similar alone fills the row when TMDB has no curated recommendations`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(id = 1, recommendations = null, similar = listResponse(7, 8)),
        )
        assertEquals(listOf(7L, 8L), detail.recommendations.map { it.id })
    }

    @Test
    fun `certification prefers the theatrical entry for the requested region`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                releaseDates = ReleaseDatesDto(
                    listOf(
                        ReleaseDateCountryDto(
                            country = "GB",
                            releases = listOf(
                                ReleaseDateDto(certification = "15-digital", type = 4),
                                ReleaseDateDto(certification = "15", type = 3),
                            ),
                        ),
                        ReleaseDateCountryDto("US", listOf(ReleaseDateDto("PG-13", type = 3))),
                    ),
                ),
            ),
            region = "GB",
        )

        assertEquals("15", detail.certification)
    }

    @Test
    fun `certification falls back to US when the region has no entry`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                releaseDates = ReleaseDatesDto(
                    listOf(ReleaseDateCountryDto("US", listOf(ReleaseDateDto("R", type = 3)))),
                ),
            ),
            region = "ID",
        )

        assertEquals("R", detail.certification)
    }

    @Test
    fun `blank certifications are skipped rather than shown as empty chips`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                releaseDates = ReleaseDatesDto(
                    listOf(
                        ReleaseDateCountryDto(
                            country = "US",
                            releases = listOf(
                                ReleaseDateDto(certification = "", type = 3),
                                ReleaseDateDto(certification = "NC-17", type = 5),
                            ),
                        ),
                    ),
                ),
            ),
            region = "US",
        )

        assertEquals("NC-17", detail.certification)
    }

    @Test
    fun `watch providers resolve to the requested region and sort by display priority`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                watchProviders = WatchProvidersDto(
                    mapOf(
                        "ID" to WatchProviderCountryDto(
                            link = "https://tmdb/watch",
                            flatrate = listOf(
                                ProviderDto(providerId = 2, providerName = "Second", displayPriority = 9),
                                ProviderDto(providerId = 1, providerName = "First", displayPriority = 1),
                            ),
                        ),
                    ),
                ),
            ),
            region = "ID",
        )

        val providers = assertNotNull(detail.watchProviders)
        assertEquals("ID", providers.region)
        assertEquals(listOf("First", "Second"), providers.streaming.map { it.name })
    }

    @Test
    fun `watch providers are null when the region has no availability at all`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(id = 1, watchProviders = WatchProvidersDto(emptyMap())),
            region = "ID",
        )
        assertNull(detail.watchProviders)
    }

    @Test
    fun `an empty provider country is treated as no availability, not an empty section`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                watchProviders = WatchProvidersDto(mapOf("US" to WatchProviderCountryDto(link = "x"))),
            ),
            region = "US",
        )
        assertNull(detail.watchProviders)
    }

    @Test
    fun `imdb id and keywords are carried through, blanks dropped`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                keywords = KeywordsDto(listOf(KeywordDto(1, "heist"), KeywordDto(2, "   "))),
                externalIds = ExternalIdsDto(imdbId = "tt0113277"),
            ),
        )

        assertEquals(listOf("heist"), detail.keywords)
        assertEquals("https://www.imdb.com/title/tt0113277/", detail.imdbUrl)
    }

    @Test
    fun `a blank imdb id yields no link rather than a broken one`() {
        val detail = MovieMapper.toDetail(MovieDetailResponse(id = 1, externalIds = ExternalIdsDto(imdbId = "")))
        assertNull(detail.imdbId)
        assertNull(detail.imdbUrl)
    }

    // ── images, translations, alternative titles ────────────────────────────

    @Test
    fun `the best-rated logo wins, with width breaking ties`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                images = ImagesDto(
                    logos = listOf(
                        ImageDto(filePath = "/poor.png", voteAverage = 1.0, width = 2000),
                        ImageDto(filePath = "/narrow.png", voteAverage = 5.0, width = 500),
                        ImageDto(filePath = "/best.png", voteAverage = 5.0, width = 1400),
                    ),
                ),
            ),
        )

        assertEquals("https://image.tmdb.org/t/p/w500/best.png", detail.logoUrl)
    }

    @Test
    fun `no logo is not an error — most titles have none`() {
        assertNull(MovieMapper.toDetail(MovieDetailResponse(id = 1)).logoUrl)
    }

    @Test
    fun `alternative titles drop the primary title and duplicates`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                title = "Heat",
                alternativeTitles = AlternativeTitlesDto(
                    listOf(
                        AlternativeTitleDto(title = "Heat"),        // same as primary
                        AlternativeTitleDto(title = "Fuego"),
                        AlternativeTitleDto(title = "fuego"),       // case-duplicate
                        AlternativeTitleDto(title = "   "),
                    ),
                ),
            ),
        )

        assertEquals(listOf("Fuego"), detail.alternativeTitles)
    }

    private fun translated(language: String, overview: String = "", tagline: String = "") =
        MovieDetailResponse(
            id = 1,
            overview = "English overview",
            tagline = "English tagline",
            translations = TranslationsDto(
                listOf(TranslationDto(language = language, data = TranslationDataDto(overview = overview, tagline = tagline))),
            ),
        )

    @Test
    fun `a device-language translation replaces the english copy`() {
        val detail = MovieMapper.toDetail(
            translated("id", overview = "Ringkasan", tagline = "Slogan"),
            language = "id",
        )

        assertEquals("Ringkasan", detail.overview)
        assertEquals("Slogan", detail.tagline)
        assertTrue(detail.isLocalised)
    }

    @Test
    fun `a partial translation localises only the fields it actually has`() {
        val detail = MovieMapper.toDetail(translated("id", overview = "Ringkasan"), language = "id")

        assertEquals("Ringkasan", detail.overview)
        // Community translations are frequently partial; the English tagline
        // beats showing nothing.
        assertEquals("English tagline", detail.tagline)
        assertTrue(detail.isLocalised)
    }

    @Test
    fun `an empty translation record leaves the english copy alone`() {
        val detail = MovieMapper.toDetail(translated("id"), language = "id")

        assertEquals("English overview", detail.overview)
        assertFalse(detail.isLocalised)
    }

    @Test
    fun `an english device skips translation lookup entirely`() {
        val detail = MovieMapper.toDetail(
            translated("en", overview = "Should not be used"),
            language = "en",
        )
        assertEquals("English overview", detail.overview)
        assertFalse(detail.isLocalised)
    }

    @Test
    fun `a language with no translation falls back to english`() {
        val detail = MovieMapper.toDetail(translated("fr", overview = "Resume"), language = "de")

        assertEquals("English overview", detail.overview)
        assertFalse(detail.isLocalised)
    }

    // ── reviews ─────────────────────────────────────────────────────────────

    @Test
    fun `reviews map author, rating and date, dropping empty bodies`() {
        val page = MovieMapper.toReviewPage(
            ReviewListResponse(
                page = 1,
                totalPages = 3,
                totalResults = 42,
                results = listOf(
                    ReviewDto(
                        id = "a",
                        author = "Critic",
                        content = "Terrific.",
                        createdAt = "2024-03-02T10:00:00.000Z",
                        authorDetails = AuthorDetailsDto(rating = 9.0, avatarPath = "/face.jpg"),
                    ),
                    ReviewDto(id = "b", author = "Silent", content = "   "),
                ),
            ),
        )

        val review = page.reviews.single()
        assertEquals("Critic", review.author)
        assertEquals(9.0, review.rating)
        assertEquals(LocalDate.of(2024, 3, 2), review.createdAt)
        assertEquals("https://image.tmdb.org/t/p/w185/face.jpg", review.avatarUrl)
        assertTrue(page.hasMore)
    }

    @Test
    fun `a gravatar avatar is passed through rather than sent to TMDB's cdn`() {
        val page = MovieMapper.toReviewPage(
            ReviewListResponse(
                results = listOf(
                    ReviewDto(
                        id = "a", author = "X", content = "ok",
                        authorDetails = AuthorDetailsDto(avatarPath = "/https://secure.gravatar.com/avatar/abc"),
                    ),
                ),
            ),
        )

        assertEquals("https://secure.gravatar.com/avatar/abc", page.reviews.single().avatarUrl)
    }

    @Test
    fun `a zero rating means unrated, not a score of zero`() {
        val page = MovieMapper.toReviewPage(
            ReviewListResponse(
                results = listOf(
                    ReviewDto(id = "a", author = "X", content = "ok", authorDetails = AuthorDetailsDto(rating = 0.0)),
                ),
            ),
        )
        assertNull(page.reviews.single().rating)
    }

    // ── mixed trending feed ─────────────────────────────────────────────────

    private fun trending(vararg items: TrendingItemDto) = TrendingListResponse(results = items.toList())

    @Test
    fun `each media type maps to its own shape, keeping server order`() {
        val entities = MovieMapper.toTrendingEntities(
            trending(
                TrendingItemDto(id = 1, mediaType = "movie", title = "Heat", releaseDate = "1995-12-15"),
                TrendingItemDto(id = 2, mediaType = "tv", name = "Severance", firstAirDate = "2022-02-18"),
                TrendingItemDto(id = 3, mediaType = "person", name = "Zendaya", knownForDepartment = "Acting"),
            ),
        )

        assertEquals(listOf(0, 1, 2), entities.map { it.position })
        assertEquals(listOf("Heat", "Severance", "Zendaya"), entities.map { it.title })

        val genres = mapOf(28 to "Action")
        val items = entities.mapNotNull { MovieMapper.toTrendingItem(it, genres) }
        assertIs<TrendingItem.Film>(items[0])
        assertIs<TrendingItem.Series>(items[1])
        assertIs<TrendingItem.Person>(items[2])
    }

    @Test
    fun `an unknown media type is dropped rather than rendered as an empty card`() {
        val entities = MovieMapper.toTrendingEntities(
            trending(
                TrendingItemDto(id = 1, mediaType = "movie", title = "Heat"),
                // TMDB has been seen returning types its docs do not list.
                TrendingItemDto(id = 2, mediaType = "collection", name = "Alien Collection"),
            ),
        )
        assertEquals(listOf("Heat"), entities.map { it.title })
    }

    @Test
    fun `film and series sharing an id stay distinct rows`() {
        // TMDB ids are unique only within a media type; a single-column key
        // would let one silently overwrite the other.
        val entities = MovieMapper.toTrendingEntities(
            trending(
                TrendingItemDto(id = 550, mediaType = "movie", title = "Fight Club"),
                TrendingItemDto(id = 550, mediaType = "tv", name = "Something Else"),
            ),
        )
        assertEquals(2, entities.size)
        assertEquals(setOf("movie", "tv"), entities.map { it.mediaType }.toSet())
    }

    @Test
    fun `a series carries its first air date, not a release date`() {
        val entity = MovieMapper.toTrendingEntities(
            trending(TrendingItemDto(id = 2, mediaType = "tv", name = "Severance", firstAirDate = "2022-02-18")),
        ).single()

        val series = assertIs<TrendingItem.Series>(MovieMapper.toTrendingItem(entity, emptyMap()))
        assertEquals(2022, series.firstAirYear)
    }

    @Test
    fun `a person keeps their department and known-for titles`() {
        val entity = MovieMapper.toTrendingEntities(
            trending(
                TrendingItemDto(
                    id = 3, mediaType = "person", name = "Zendaya", knownForDepartment = "Acting",
                    profilePath = "/z.jpg",
                    knownFor = listOf(
                        TrendingItemDto(id = 9, mediaType = "movie", title = "Dune"),
                        TrendingItemDto(id = 10, mediaType = "tv", name = "Euphoria"),
                    ),
                ),
            ),
        ).single()

        val person = assertIs<TrendingItem.Person>(MovieMapper.toTrendingItem(entity, emptyMap()))
        assertEquals("Acting", person.knownForDepartment)
        assertEquals(listOf("Dune", "Euphoria"), person.knownFor)
        assertEquals("https://image.tmdb.org/t/p/w185/z.jpg", person.profileUrl)
    }

    @Test
    fun `the unrated rule applies to the mixed feed too`() {
        val entity = MovieMapper.toTrendingEntities(
            trending(TrendingItemDto(id = 1, mediaType = "movie", title = "New", voteAverage = 0.0, voteCount = 0)),
        ).single()
        assertNull(entity.voteAverage)
    }

    @Test
    fun `every type exposes a tmdb url so nothing is a dead tap`() {
        val entities = MovieMapper.toTrendingEntities(
            trending(
                TrendingItemDto(id = 1, mediaType = "movie", title = "Heat"),
                TrendingItemDto(id = 2, mediaType = "tv", name = "Severance"),
                TrendingItemDto(id = 3, mediaType = "person", name = "Zendaya"),
            ),
        )
        val urls = entities.mapNotNull { MovieMapper.toTrendingItem(it, emptyMap()) }.map { it.tmdbUrl }
        assertEquals(
            listOf(
                "https://www.themoviedb.org/movie/1",
                "https://www.themoviedb.org/tv/2",
                "https://www.themoviedb.org/person/3",
            ),
            urls,
        )
    }

    // ── Tier 3: collections, people, series ─────────────────────────────────

    @Test
    fun `collection parts are ordered by release, undated last`() {
        val collection = MovieMapper.toCollection(
            CollectionResponse(
                id = 1, name = "Alien Collection",
                parts = listOf(
                    MovieDto(id = 3, title = "Alien 3", releaseDate = "1992-05-22"),
                    MovieDto(id = 99, title = "Untitled sequel", releaseDate = ""),
                    MovieDto(id = 1, title = "Alien", releaseDate = "1979-05-25"),
                    MovieDto(id = 2, title = "Aliens", releaseDate = "1986-07-18"),
                ),
            ),
            emptyMap(),
        )

        // A franchise read out of order is unreadable as a sequence, which is
        // the only reason to group them.
        assertEquals(listOf("Alien", "Aliens", "Alien 3", "Untitled sequel"), collection.parts.map { it.title })
    }

    @Test
    fun `belongs_to_collection becomes a reference on detail`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(id = 1, belongsToCollection = CollectionRefDto(id = 131292, name = "Iron Man Collection")),
        )
        assertEquals(131292L, detail.collection?.id)
        assertEquals("Iron Man Collection", detail.collection?.name)
    }

    @Test
    fun `a blank collection stub is dropped rather than shown as an empty chip`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(id = 1, belongsToCollection = CollectionRefDto(id = 0, name = "")),
        )
        assertNull(detail.collection)
    }

    @Test
    fun `filmography merges cast and crew, one entry per film, newest first`() {
        val person = MovieMapper.toPersonDetail(
            PersonResponse(
                id = 1, name = "Someone",
                movieCredits = PersonCreditsDto(
                    cast = listOf(
                        PersonCastCreditDto(id = 10, title = "Older", releaseDate = "1999-01-01", character = "Him"),
                        PersonCastCreditDto(id = 20, title = "Newer", releaseDate = "2020-01-01", character = "Her"),
                    ),
                    // Same film they also acted in: one card, not two.
                    crew = listOf(PersonCrewCreditDto(id = 20, title = "Newer", releaseDate = "2020-01-01", job = "Director")),
                ),
            ),
            emptyMap(),
        )

        assertEquals(listOf("Newer", "Older"), person.filmography.map { it.movie.title })
        // The cast credit wins the merge, so the role shown is the character.
        assertEquals("Her", person.filmography.first().role)
    }

    @Test
    fun `a living person has a birth year but no dangling range`() {
        val alive = MovieMapper.toPersonDetail(
            PersonResponse(id = 1, name = "A", birthday = "1970-04-01"), emptyMap(),
        )
        assertEquals("1970", alive.lifespan)

        val gone = MovieMapper.toPersonDetail(
            PersonResponse(id = 2, name = "B", birthday = "1930-01-01", deathday = "2000-01-01"), emptyMap(),
        )
        assertEquals("1930 – 2000", gone.lifespan)

        val unknown = MovieMapper.toPersonDetail(PersonResponse(id = 3, name = "C"), emptyMap())
        assertNull(unknown.lifespan)
    }

    @Test
    fun `series certification comes from content_ratings, falling back to US`() {
        val response = TvDetailResponse(
            id = 1, name = "Show",
            contentRatings = ContentRatingsDto(
                listOf(ContentRatingDto("US", "TV-MA"), ContentRatingDto("GB", "18")),
            ),
        )
        assertEquals("18", MovieMapper.toTvDetail(response, region = "GB").certification)
        // A region TMDB has no rating for degrades to US rather than to null.
        assertEquals("TV-MA", MovieMapper.toTvDetail(response, region = "ID").certification)
    }

    @Test
    fun `an ended series shows a closed range, a running one stays open`() {
        val ended = MovieMapper.toTvDetail(
            TvDetailResponse(id = 1, name = "S", firstAirDate = "2011-04-17", lastAirDate = "2019-05-19", status = "Ended"),
        )
        assertEquals("2011 – 2019", ended.airedRange)

        // A returning series has a last-aired date too, but printing it would
        // read as though the show had finished.
        val running = MovieMapper.toTvDetail(
            TvDetailResponse(id = 2, name = "S", firstAirDate = "2022-01-01", lastAirDate = "2024-01-01", status = "Returning Series"),
        )
        assertEquals("2022 –", running.airedRange)
    }

    @Test
    fun `series applies the unrated rule like everything else`() {
        val unrated = MovieMapper.toTvDetail(TvDetailResponse(id = 1, name = "S", voteAverage = 0.0, voteCount = 0))
        assertNull(unrated.voteAverage)
    }

    // ── videos ──────────────────────────────────────────────────────────────

    private fun video(
        key: String,
        type: String,
        official: Boolean = true,
        published: String = "2024-01-01",
        site: String = "YouTube",
    ) = VideoDto(key = key, site = site, type = type, official = official, name = key, size = 1080, publishedAt = published)

    @Test
    fun `an official trailer outranks an official teaser, whatever the dates say`() {
        // Teasers are cut months earlier and show less of the film. Ordering by
        // date alone would open a title on its teaser whenever the teaser was
        // re-published later, which happens.
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                videos = VideosDto(listOf(
                    video("teaser", "Teaser", published = "2025-06-01"),
                    video("trailer", "Trailer", published = "2024-01-01"),
                )),
            ),
            emptyMap(), "US",
        )

        assertEquals("trailer", detail.trailerKey)
        assertEquals(listOf("trailer", "teaser"), detail.videos.map { it.key })
    }

    @Test
    fun `official beats unofficial before type is even considered`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                videos = VideosDto(listOf(
                    video("fanTrailer", "Trailer", official = false),
                    video("officialTeaser", "Teaser", official = true),
                )),
            ),
            emptyMap(), "US",
        )

        assertEquals("officialTeaser", detail.trailerKey)
    }

    @Test
    fun `a video hosted anywhere but YouTube is dropped, not carried`() {
        // The player can only play YouTube keys, so a Vimeo key would reach it
        // and fail. TMDB has only ever returned YouTube here; this is the guard
        // for the day it does not.
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(
                id = 1,
                videos = VideosDto(listOf(
                    video("vimeoOne", "Trailer", site = "Vimeo"),
                    video("youtubeOne", "Clip"),
                )),
            ),
            emptyMap(), "US",
        )

        assertEquals(listOf("youtubeOne"), detail.videos.map { it.key })
        // Carried as a video, but a clip is not what a "Watch trailer" button
        // should play — see pickTrailerKey.
        assertNull(detail.trailerKey)
    }

    @Test
    fun `a video with no key is dropped`() {
        val detail = MovieMapper.toDetail(
            MovieDetailResponse(id = 1, videos = VideosDto(listOf(video("", "Trailer")))),
            emptyMap(), "US",
        )

        assertEquals(emptyList(), detail.videos)
        assertEquals(null, detail.trailerKey)
    }
}
