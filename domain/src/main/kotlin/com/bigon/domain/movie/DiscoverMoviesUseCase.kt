package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.MoviePage
import javax.inject.Inject

/** Genre- and provider-filtered browsing; null on either means unfiltered. */
class DiscoverMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(
        genreId: Int?,
        page: Int = 1,
        streamingProviderId: Int? = null,
    ): AppResult<MoviePage> = repository.discover(genreId, page, streamingProviderId)
}
