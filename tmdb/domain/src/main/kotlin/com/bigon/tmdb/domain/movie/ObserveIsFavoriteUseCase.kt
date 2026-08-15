package com.bigon.tmdb.domain.movie

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveIsFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(movieId: Long): Flow<Boolean> = repository.observeIsFavorite(movieId)
}
