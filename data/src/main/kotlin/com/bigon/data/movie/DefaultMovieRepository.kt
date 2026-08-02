package com.bigon.data.movie

import com.bigon.core.common.AppResult
import com.bigon.core.common.DispatcherProvider
import com.bigon.core.common.map
import com.bigon.core.database.GenreDao
import com.bigon.core.database.MovieDao
import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import com.bigon.core.model.MovieDetail
import com.bigon.core.model.MoviePage
import com.bigon.core.network.ApiCaller
import com.bigon.domain.movie.LoadMoreOutcome
import com.bigon.domain.movie.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first: the database is the single source of truth. Reads never touch
 * the network, and a failed refresh leaves the cached list untouched — the user
 * keeps seeing content, and the caller decides how to surface the failure.
 *
 * Genre names are joined in Kotlin rather than SQL: ids live on the movie row
 * and names in their own table, so both flows are combined and resolved here.
 */
@Singleton
class DefaultMovieRepository @Inject constructor(
    private val movieDao: MovieDao,
    private val genreDao: GenreDao,
    private val movieApi: MovieApi,
    private val apiCaller: ApiCaller,
    private val dispatchers: DispatcherProvider,
) : MovieRepository {

    override fun observeCategory(category: MovieCategory): Flow<List<Movie>> =
        combine(
            movieDao.observeByList(category.listKey),
            genreDao.observeAll(),
        ) { rows, genres ->
            val genresById = genres.associate { it.id to it.name }
            rows.map { MovieMapper.toDomain(it, genresById) }
        }.flowOn(dispatchers.io)

    override fun observeCached(movieId: Long): Flow<Movie?> =
        combine(
            movieDao.observeById(movieId),
            genreDao.observeAll(),
        ) { row, genres ->
            row?.let { MovieMapper.toDomain(it, genres.associate { g -> g.id to g.name }) }
        }.flowOn(dispatchers.io)

    override suspend fun detail(movieId: Long): AppResult<MovieDetail> =
        withContext(dispatchers.io) {
            apiCaller.execute { movieApi.detail(movieId) }.map(MovieMapper::toDetail)
        }

    /** total_pages per list, learned from responses. In-memory only: after
     *  process death the next loadMore simply asks TMDB and finds out. */
    private val totalPagesByList = mutableMapOf<String, Int>()

    override suspend fun refresh(category: MovieCategory): AppResult<Unit> =
        withContext(dispatchers.io) {
            // Genre names are a prerequisite for rendering, but a stale or empty
            // genre table must not fail the whole refresh.
            refreshGenresIfNeeded()

            apiCaller.execute { movieApi.fetch(category, page = 1) }
                .map { response ->
                    totalPagesByList[category.listKey] = response.totalPages
                    val entities = response.results.mapIndexed { index, dto ->
                        MovieMapper.toEntity(dto, category.listKey, page = 1, position = index)
                    }
                    movieDao.replaceList(category.listKey, entities)
                }
        }

    override suspend fun loadMore(category: MovieCategory): AppResult<LoadMoreOutcome> =
        withContext(dispatchers.io) {
            val listKey = category.listKey
            val lastPage = movieDao.maxPage(listKey)
                // Nothing cached yet: loading "more" of nothing is a refresh.
                ?: return@withContext refresh(category).map { LoadMoreOutcome.Appended }

            totalPagesByList[listKey]?.let { total ->
                if (lastPage >= total) return@withContext AppResult.Success(LoadMoreOutcome.EndReached)
            }

            apiCaller.execute { movieApi.fetch(category, page = lastPage + 1) }
                .map { response ->
                    totalPagesByList[listKey] = response.totalPages
                    if (response.results.isEmpty()) {
                        LoadMoreOutcome.EndReached
                    } else {
                        // Positions continue after the cached rows so ordering
                        // survives duplicates being dropped by the DAO.
                        val base = (movieDao.maxPosition(listKey) ?: -1) + 1
                        val entities = response.results.mapIndexed { index, dto ->
                            MovieMapper.toEntity(dto, listKey, page = lastPage + 1, position = base + index)
                        }
                        movieDao.appendList(entities)
                        LoadMoreOutcome.Appended
                    }
                }
        }

    override fun observeGenres(): Flow<List<Genre>> =
        genreDao.observeAll()
            .map { rows -> rows.map { Genre(id = it.id, name = it.name) } }
            .flowOn(dispatchers.io)

    override suspend fun search(query: String, page: Int): AppResult<MoviePage> =
        withContext(dispatchers.io) {
            refreshGenresIfNeeded()
            val genresById = genreDao.getAll().associate { it.id to it.name }
            apiCaller.execute { movieApi.search(query, page = page) }
                .map { response -> MovieMapper.toPage(response, genresById) }
        }

    override suspend fun discover(genreId: Int?, page: Int): AppResult<MoviePage> =
        withContext(dispatchers.io) {
            refreshGenresIfNeeded()
            val genresById = genreDao.getAll().associate { it.id to it.name }
            apiCaller.execute { movieApi.discover(withGenres = genreId?.toString(), page = page) }
                .map { response -> MovieMapper.toPage(response, genresById) }
        }

    override suspend fun clearCatalogCache() = withContext(dispatchers.io) {
        movieDao.clearAll()
        // Genres are cheap to refetch and refetched lazily on next use.
    }

    private suspend fun refreshGenresIfNeeded() {
        if (genreDao.count() > 0) return
        apiCaller.execute { movieApi.genres() }
            .map { response -> genreDao.upsertAll(response.genres.map(MovieMapper::toGenreEntity)) }
    }
}

/** Stable cache key for a category — deliberately not the enum's ordinal. */
internal val MovieCategory.listKey: String
    get() = when (this) {
        MovieCategory.Trending -> "trending"
        MovieCategory.Popular -> "popular"
        MovieCategory.NowPlaying -> "now_playing"
        MovieCategory.TopRated -> "top_rated"
        MovieCategory.Upcoming -> "upcoming"
    }

private suspend fun MovieApi.fetch(category: MovieCategory, page: Int): MovieListResponse = when (category) {
    MovieCategory.Trending -> trending(page)
    MovieCategory.Popular -> popular(page)
    MovieCategory.NowPlaying -> nowPlaying(page)
    MovieCategory.TopRated -> topRated(page)
    MovieCategory.Upcoming -> upcoming(page)
}
