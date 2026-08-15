package com.bigon.tmdb.domain.movie

import com.bigon.tmdb.model.Movie
import javax.inject.Inject

class SetFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(movie: Movie, favorite: Boolean) =
        repository.setFavorite(movie, favorite)
}
