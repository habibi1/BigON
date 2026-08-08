package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MovieCollection
import com.bigon.core.model.PersonDetail
import com.bigon.core.model.TvDetail
import javax.inject.Inject

/** A franchise and its parts. */
class GetCollectionUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(collectionId: Long): AppResult<MovieCollection> =
        repository.collection(collectionId)
}

/** A person and their filmography. */
class GetPersonUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(personId: Long): AppResult<PersonDetail> = repository.person(personId)
}

/** Series detail. */
class GetTvDetailUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(tvId: Long): AppResult<TvDetail> = repository.tvDetail(tvId)
}
