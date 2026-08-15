package com.bigon.tmdb.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.tmdb.model.MovieCollection
import com.bigon.tmdb.model.PersonDetail
import com.bigon.tmdb.model.TvDetail
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
