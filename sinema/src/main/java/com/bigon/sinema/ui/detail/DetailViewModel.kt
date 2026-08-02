package com.bigon.sinema.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.GetMovieDetailUseCase
import com.bigon.domain.movie.ObserveCachedMovieUseCase
import com.bigon.domain.movie.ObserveIsFavoriteUseCase
import com.bigon.domain.movie.SetFavoriteUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The movie id is a runtime argument, so this ViewModel is assisted-injected —
 * Hilt still owns every other dependency.
 */
@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val movieId: Long,
    observeCachedMovie: ObserveCachedMovieUseCase,
    observeIsFavorite: ObserveIsFavoriteUseCase,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val setFavorite: SetFavoriteUseCase,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(movieId: Long): DetailViewModel
    }

    private val _state = MutableStateFlow(DetailUiState(movieId = movieId))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        tracker.track(AnalyticsEvent.ScreenView("movie_detail"))

        // Cached row first, so the shared element lands on real content rather
        // than a spinner while the network request is still in flight.
        observeCachedMovie(movieId)
            .onEach { movie -> _state.update { it.copy(cached = movie) } }
            .launchIn(viewModelScope)

        observeIsFavorite(movieId)
            .onEach { favorite -> _state.update { it.copy(isFavorite = favorite) } }
            .launchIn(viewModelScope)

        load()
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.Retry -> load()
            is DetailIntent.FavoriteToggled -> {
                val snapshot = _state.value.asMovie() ?: return
                viewModelScope.launch { setFavorite(snapshot, intent.favorite) }
            }
            // Back is navigation, owned by the caller.
            DetailIntent.Back -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getMovieDetail(movieId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, detail = result.value, error = null)
                }
                // A failure never clears cached content already on screen.
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }
}
