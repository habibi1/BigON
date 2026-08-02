package com.bigon.sinema.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.model.MovieCategory
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.LoadMoreMoviesUseCase
import com.bigon.domain.movie.LoadMoreOutcome
import com.bigon.domain.movie.ObserveMoviesUseCase
import com.bigon.domain.movie.RefreshMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Offline-first in practice: switching category immediately subscribes to the
 * cached list, then refreshes in the background. A failed refresh surfaces an
 * error without clearing whatever was already cached.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeMovies: ObserveMoviesUseCase,
    private val refreshMovies: RefreshMoviesUseCase,
    private val loadMoreMovies: LoadMoreMoviesUseCase,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    /** Cancelled and replaced whenever the category changes. */
    private var observeJob: Job? = null

    init {
        tracker.track(AnalyticsEvent.ScreenView("home"))
        selectCategory(MovieCategory.Default)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh(_state.value.category)
            HomeIntent.LoadMore -> loadMore()
            is HomeIntent.CategorySelected -> selectCategory(intent.category)
            is HomeIntent.MovieClicked -> {
                tracker.track(AnalyticsEvent.ScreenView("movie_detail"))
                _effects.trySend(HomeEffect.NavigateToDetail(intent.movie.id))
            }
            HomeIntent.ErrorDismissed -> _state.update { it.copy(error = null) }
        }
    }

    private fun selectCategory(category: MovieCategory) {
        // Re-selecting the active category is a manual refresh — the only
        // affordance needed after Settings clears the catalogue cache.
        if (observeJob != null && _state.value.category == category) {
            refresh(category)
            return
        }

        _state.update {
            it.copy(category = category, movies = emptyList(), error = null, endReached = false, isAppending = false)
        }

        observeJob?.cancel()
        observeJob = observeMovies(category)
            .onEach { movies -> _state.update { it.copy(movies = movies) } }
            .launchIn(viewModelScope)

        refresh(category)
    }

    private fun refresh(category: MovieCategory) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null, endReached = false) }
            val result = refreshMovies(category)
            _state.update { current ->
                current.copy(
                    isRefreshing = false,
                    error = (result as? AppResult.Failure)?.error?.toUiText(),
                )
            }
        }
    }

    private fun loadMore() {
        val snapshot = _state.value
        if (!snapshot.canLoadMore) return
        viewModelScope.launch {
            _state.update { it.copy(isAppending = true) }
            when (val result = loadMoreMovies(snapshot.category)) {
                is AppResult.Success -> _state.update {
                    it.copy(isAppending = false, endReached = result.value == LoadMoreOutcome.EndReached)
                }
                // Appending is a background nicety: fail quietly, allow retrying
                // by scrolling again rather than interrupting with a banner.
                is AppResult.Failure -> _state.update { it.copy(isAppending = false) }
            }
        }
    }
}
