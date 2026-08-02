package com.bigon.domain.movie

import com.bigon.core.model.Movie
import javax.inject.Inject

class SetFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    suspend operator fun invoke(movie: Movie, favorite: Boolean) =
        repository.setFavorite(movie, favorite)
}
