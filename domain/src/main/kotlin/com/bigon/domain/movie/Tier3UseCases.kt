package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MovieCollection
import javax.inject.Inject

/** A franchise and its parts. */
class GetCollectionUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(collectionId: Long): AppResult<MovieCollection> =
        repository.collection(collectionId)
}
