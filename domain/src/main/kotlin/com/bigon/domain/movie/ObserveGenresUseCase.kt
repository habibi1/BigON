package com.bigon.domain.movie

import com.bigon.core.model.Genre
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGenresUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(): Flow<List<Genre>> = repository.observeGenres()
}
