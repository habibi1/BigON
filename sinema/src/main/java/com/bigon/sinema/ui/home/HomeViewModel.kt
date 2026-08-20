package com.bigon.sinema.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.tmdb.model.MovieCategory
import com.bigon.tmdb.model.TrendingItem
import com.bigon.tmdb.model.typeLabel
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.toUiText
import com.bigon.tmdb.domain.movie.LoadMoreMoviesUseCase
import com.bigon.tmdb.domain.movie.LoadMoreOutcome
import com.bigon.tmdb.domain.movie.ObserveMoviesUseCase
import com.bigon.tmdb.domain.movie.ObserveTrendingAllUseCase
import com.bigon.tmdb.domain.movie.RefreshTrendingAllUseCase
import com.bigon.tmdb.domain.movie.RefreshMoviesUseCase
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
    private val observeTrendingAll: ObserveTrendingAllUseCase,
    private val refreshTrendingAll: RefreshTrendingAllUseCase,
    private val loadMoreMovies: LoadMoreMoviesUseCase,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    /** Cancelled and replaced whenever the feed changes. */
    private var observeJob: Job? = null

    /**
     * True between asking for a wholesale replacement — a new feed, or a
     * refresh that rebuilds from page 1 — and the emission that delivers it.
     * Only that emission moves the viewport; a page being appended must not,
     * or scrolling would fight the reader.
     */
    private var awaitingContentReplacement = false

    private fun HomeUiState.nextGeneration(): Int =
        if (awaitingContentReplacement) {
            awaitingContentReplacement = false
            contentGeneration + 1
        } else {
            contentGeneration
        }

    init {
        tracker.track(AnalyticsEvent.ScreenView("home"))
        selectFeed(HomeFeed.Category(MovieCategory.Default))
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> {
                // Only a pull raises the pull indicator; every other refresh
                // path leaves it alone.
                _state.update { it.copy(isPullRefreshing = true) }
                refreshCurrent()
            }
            HomeIntent.LoadMore -> loadMore()
            is HomeIntent.FeedSelected -> selectFeed(intent.feed)
            is HomeIntent.MovieClicked -> {
                tracker.track(AnalyticsEvent.MovieOpened(intent.movie.id, source = "home"))
                _effects.trySend(HomeEffect.NavigateToDetail(intent.movie.id))
            }
            is HomeIntent.TrendingItemClicked -> when (val item = intent.item) {
                // A film has a real destination; the other two do not yet, so
                // they hand off to TMDB rather than pretending.
                is TrendingItem.Film -> {
                    tracker.track(AnalyticsEvent.MovieOpened(item.id, source = "trending_all"))
                    _effects.trySend(HomeEffect.NavigateToDetail(item.id))
                }
                is TrendingItem.Series -> _effects.trySend(HomeEffect.NavigateToTv(item.id))
                is TrendingItem.Person -> _effects.trySend(HomeEffect.NavigateToPerson(item.id))
            }
            HomeIntent.ErrorDismissed -> _state.update { it.copy(error = null) }
        }
    }

    private fun refreshCurrent() {
        when (val feed = _state.value.feed) {
            is HomeFeed.Category -> refresh(feed.category)
            HomeFeed.AcrossTypes -> refreshAcrossTypes()
        }
    }

    private fun selectFeed(feed: HomeFeed) {
        // Re-selecting the active feed used to mean "refresh". It no longer
        // reaches here — the screen treats that tap as scroll-to-top — but the
        // guard stays so a re-selection can never rebuild the observer and
        // wipe the list as a side effect.
        if (observeJob != null && _state.value.feed == feed) return

        // The outgoing feed's films stay on screen until the new ones arrive.
        // Clearing here instead put the grid through an empty frame, which
        // collapsed the scroll position and flashed skeletons over content that
        // was about to be replaced anyway.
        _state.update {
            it.copy(
                feed = feed,
                error = null,
                endReached = false,
                isAppending = false,
            )
        }
        awaitingContentReplacement = true

        observeJob?.cancel()
        when (feed) {
            is HomeFeed.Category -> {
                observeJob = observeMovies(feed.category)
                    .onEach { movies ->
                        _state.update { it.copy(movies = movies, contentGeneration = it.nextGeneration()) }
                    }
                    .launchIn(viewModelScope)
                refresh(feed.category)
            }

            HomeFeed.AcrossTypes -> {
                observeJob = observeTrendingAll()
                    .onEach { items ->
                        _state.update { it.copy(trendingItems = items, contentGeneration = it.nextGeneration()) }
                    }
                    .launchIn(viewModelScope)
                refreshAcrossTypes()
            }
        }
    }

    private fun refreshAcrossTypes() {
        viewModelScope.launch {
            awaitingContentReplacement = true
            _state.update { it.copy(isRefreshing = true, error = null, endReached = true) }
            val result = refreshTrendingAll()
            _state.update { current ->
                current.copy(
                    isRefreshing = false,
                    isPullRefreshing = false,
                    error = (result as? AppResult.Failure)?.error?.toUiText(),
                )
            }
        }
    }

    private fun refresh(category: MovieCategory) {
        viewModelScope.launch {
            awaitingContentReplacement = true
            _state.update { it.copy(isRefreshing = true, error = null, endReached = false) }
            val result = refreshMovies(category)
            _state.update { current ->
                current.copy(
                    isRefreshing = false,
                    isPullRefreshing = false,
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
            val category = snapshot.category ?: return@launch
            when (val result = loadMoreMovies(category)) {
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
