package com.bigon.domain.movie

import com.bigon.core.common.AppResult
import com.bigon.core.model.TrendingItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The cached mixed trending feed. Emits immediately; never touches the network. */
class ObserveTrendingAllUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    operator fun invoke(): Flow<List<TrendingItem>> = repository.observeTrendingAll()
}

/** Refreshes that feed, reporting failure separately so the cache survives it. */
class RefreshTrendingAllUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshTrendingAll()
}
