package com.bigon.data.movie

import com.bigon.core.common.DispatcherProvider
import com.bigon.core.database.FavoriteDao
import com.bigon.core.database.FavoriteEntity
import com.bigon.core.model.Movie
import com.bigon.domain.movie.FavoritesRepository
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
            if (favorite) favoriteDao.upsert(movie.toSnapshot(addedAt = clock())) else favoriteDao.delete(movie.id)
        }

    private fun Movie.toSnapshot(addedAt: Long) = FavoriteEntity(
        id = id,
        title = title,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate?.toString(),
        voteAverage = voteAverage,
        genreNames = genres,
        addedAt = addedAt,
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
