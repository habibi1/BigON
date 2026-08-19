package com.bigon.tmdb.domain.movie

import com.bigon.tmdb.model.Genre
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGenresUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(): Flow<List<Genre>> = repository.observeGenres()
}
