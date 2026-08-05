package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.WatchProvider
import javax.inject.Inject

/**
 * The streaming services available where the user is. Fetched once per session
 * by the browse screen; the catalogue changes on the scale of months, not
 * minutes, so a failure here degrades to "no filter row" rather than an error.
 */
class GetStreamingServicesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): AppResult<List<WatchProvider>> = repository.streamingServices()
}
