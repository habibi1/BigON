package com.bigon.sinema.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.UiText
import com.bigon.core.ui.toUiText
import com.bigon.tmdb.domain.movie.GetMovieDetailUseCase
import com.bigon.tmdb.domain.movie.GetMovieReviewsUseCase
import com.bigon.tmdb.domain.movie.ObserveCachedMovieUseCase
import com.bigon.tmdb.domain.movie.ObserveIsFavoriteUseCase
import com.bigon.tmdb.domain.movie.SetFavoriteUseCase
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
    private val getMovieReviews: GetMovieReviewsUseCase,
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
            .onEach { movie -> _state.update { it.copy(cached = movie).pinArtwork() } }
            .launchIn(viewModelScope)

        observeIsFavorite(movieId)
            .onEach { favorite -> _state.update { it.copy(isFavorite = favorite) } }
            .launchIn(viewModelScope)

        load()
        loadReviews(page = 1)
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.Retry -> load()
            is DetailIntent.FavoriteToggled -> {
                val snapshot = _state.value.asMovie() ?: return
                viewModelScope.launch { setFavorite(snapshot, intent.favorite) }
            }

            is DetailIntent.RecommendationClicked ->
                tracker.track(AnalyticsEvent.MovieOpened(intent.movieId, source = "recommendation"))

            DetailIntent.TrailerUnavailable ->
                _state.update { it.copy(error = UiText.Dynamic("No app on this device can open the trailer.")) }

            DetailIntent.ReviewsRequested -> loadReviews(page = 1)
            DetailIntent.MoreReviewsRequested ->
                loadReviews(page = _state.value.reviewsPage + 1)

            // Back and opening a link are navigation, owned by the caller.
            DetailIntent.Back -> Unit
        }
    }

    /**
     * Reviews load with the screen rather than on demand. They are still a
     * separate request — the endpoint is separate — so a failure here leaves
     * detail intact, and the guard keeps a retry or a fast scroll from issuing
     * a page already held.
     */
    private fun loadReviews(page: Int) {
        val current = _state.value
        if (current.isLoadingReviews) return
        if (page == 1 && current.reviews.isNotEmpty()) return

        _state.update { it.copy(isLoadingReviews = true, reviewsError = null) }
        viewModelScope.launch {
            when (val result = getMovieReviews(movieId, page)) {
                is AppResult.Success -> _state.update { state ->
                    val loaded = result.value
                    state.copy(
                        isLoadingReviews = false,
                        // Appending rather than replacing, deduplicated: TMDB
                        // occasionally repeats a review across page boundaries.
                        reviews = (state.reviews + loaded.reviews).distinctBy { it.id },
                        reviewsPage = loaded.page,
                        hasMoreReviews = loaded.hasMore,
                        totalReviews = loaded.totalResults,
                        reviewsError = null,
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(isLoadingReviews = false, reviewsError = result.error.toUiText())
                }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getMovieDetail(movieId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, detail = result.value, error = null).pinArtwork()
                }
                // A failure never clears cached content already on screen.
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }

    /**
     * Claims the hero artwork for the first source that has any, and never
     * lets go. Both sources keep arriving after that — the detail response,
     * and the cached row again once the repository writes the response back
     * over it — and each one is a chance for the image to change under the
     * reader for no reason they can see. See [DetailUiState.paintedPosterUrl].
     */
    private fun DetailUiState.pinArtwork(): DetailUiState = copy(
        paintedPosterUrl = paintedPosterUrl ?: posterUrl,
        paintedBackdropUrl = paintedBackdropUrl ?: backdropUrl,
    )
}
