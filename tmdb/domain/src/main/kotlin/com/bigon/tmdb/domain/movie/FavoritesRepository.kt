package com.bigon.tmdb.domain.movie

import com.bigon.tmdb.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Favourites are local-only by design: TMDB's account endpoints need a user
 * session, and offline-first favourites are the better product for this app.
 * Stored as self-contained snapshots, so they outlive every cache.
 */
interface FavoritesRepository {

    /** Newest first. Main-safe; never throws. */
    fun observeFavorites(): Flow<List<Movie>>

    fun observeIsFavorite(movieId: Long): Flow<Boolean>

    /** Idempotent: favouriting twice or unfavouriting a non-favourite is a no-op. */
    suspend fun setFavorite(movie: Movie, favorite: Boolean)
}
