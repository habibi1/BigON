package com.bigon.tmdb.domain.movie

import com.bigon.tmdb.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Favourites are local-only by design: TMDB's account endpoints need a user
 * session, and offline-first favourites are the better product for this app.
 * Stored as self-contained snapshots, so they outlive every cache — but not
 * indefinitely: the snapshot is TMDB content, and TMDB's terms cap how long it
 * may be held, so it is periodically refreshed in place.
 */
interface FavoritesRepository {

    /** Newest first. Main-safe; never throws. */
    fun observeFavorites(): Flow<List<Movie>>

    fun observeIsFavorite(movieId: Long): Flow<Boolean>

    /** Idempotent: favouriting twice or unfavouriting a non-favourite is a no-op. */
    suspend fun setFavorite(movie: Movie, favorite: Boolean)

    /**
     * Favourites whose cached TMDB content has aged past what TMDB's terms
     * allow, oldest first. The favourite itself never expires — it is the
     * user's record — but the title and artwork stored alongside it must be
     * refetched. See [RefreshStaleFavoritesUseCase].
     */
    suspend fun staleSnapshotIds(): List<Long>
}
