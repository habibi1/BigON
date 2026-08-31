package com.bigon.tmdb.data.movie

import com.bigon.core.common.DispatcherProvider
import com.bigon.tmdb.database.FavoriteDao
import com.bigon.tmdb.database.FavoriteEntity
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.domain.movie.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val dispatchers: DispatcherProvider,
) : FavoritesRepository {

    /** Injectable time source; tests override for deterministic ordering. */
    internal var clock: () -> Long = System::currentTimeMillis

    override fun observeFavorites(): Flow<List<Movie>> =
        favoriteDao.observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override fun observeIsFavorite(movieId: Long): Flow<Boolean> =
        favoriteDao.observeIsFavorite(movieId).flowOn(dispatchers.io)

    override suspend fun setFavorite(movie: Movie, favorite: Boolean) =
        withContext(dispatchers.io) {
            if (favorite) favoriteDao.upsert(movie.toSnapshot(now = clock())) else favoriteDao.delete(movie.id)
        }

    override suspend fun staleSnapshotIds(): List<Long> = withContext(dispatchers.io) {
        favoriteDao.staleIds(TmdbCachePolicy.cutoff(clock()))
    }

    /**
     * Both timestamps start together, then diverge: [FavoriteEntity.addedAt]
     * stays put for as long as the favourite exists, while
     * [FavoriteEntity.snapshotAt] moves each time the TMDB content is refetched.
     */
    private fun Movie.toSnapshot(now: Long) = FavoriteEntity(
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate?.toString(),
        voteAverage = voteAverage,
        genreNames = genres,
        addedAt = now,
        snapshotAt = now,
    )

    private fun FavoriteEntity.toDomain() = Movie(
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        },
        voteAverage = voteAverage,
        genres = genreNames,
    )
}
