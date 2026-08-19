package com.bigon.sinema.ui.favorites

import com.bigon.tmdb.model.Movie

data class FavoritesUiState(
    val favorites: List<Movie> = emptyList(),
    /** Distinguishes "loading" from a genuinely empty collection. */
    val isLoaded: Boolean = false,
) {
    val showEmptyState: Boolean get() = isLoaded && favorites.isEmpty()
}

sealed interface FavoritesIntent {
    data class MovieClicked(val movie: Movie) : FavoritesIntent
    data object BrowseTrending : FavoritesIntent
}

sealed interface FavoritesEffect {
    data class NavigateToDetail(val movieId: Long) : FavoritesEffect
    data object NavigateToHome : FavoritesEffect
}
