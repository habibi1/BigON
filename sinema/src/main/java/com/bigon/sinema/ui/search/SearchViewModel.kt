package com.bigon.sinema.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlagRepository
import com.bigon.core.config.Flags
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.DiscoverMoviesUseCase
import com.bigon.domain.movie.ObserveGenresUseCase
import com.bigon.domain.movie.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Typing is debounced by `Flags.SearchDebounceMs`; [mapLatest] cancels the
 * in-flight request the moment a newer input arrives, so a slow response for
 * "du" can never overwrite the results for "dune".
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    observeGenres: ObserveGenresUseCase,
    private val searchMovies: SearchMoviesUseCase,
    private val discoverMovies: DiscoverMoviesUseCase,
    flags: FeatureFlagRepository,
    tracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects: Flow<SearchEffect> = _effects.receiveAsFlow()

    private val queryInput = MutableStateFlow("")
    private val genreInput = MutableStateFlow<Int?>(null)
    private val retries = MutableStateFlow(0)

    private val debounceMillis = flags.get(Flags.SearchDebounceMs).toLong()

    init {
        tracker.track(AnalyticsEvent.ScreenView("search"))

        observeGenres()
            .onEach { genres -> _state.update { it.copy(genres = genres) } }
            .launchIn(viewModelScope)

        queryInput
            // Clearing the field should react instantly; only typing is debounced.
            .debounce { input -> if (input.isBlank()) 0L else debounceMillis }
            .combine(genreInput) { query, genre -> query.trim() to genre }
            // Retries re-emit the same request; StateFlow inputs dedupe themselves.
            .combine(retries) { request, _ -> request }
            .mapLatest { (query, genreId) ->
                _state.update { it.copy(isSearching = true, error = null) }
                if (query.isBlank()) discoverMovies(genreId) else searchMovies(query)
            }
            .onEach { result ->
                when (result) {
                    // A new request always replaces: page 1 of a different query.
                    is AppResult.Success -> _state.update {
                        it.copy(
                            isSearching = false,
                            results = result.value.movies,
                            page = result.value.page,
                            totalPages = result.value.totalPages,
                        )
                    }
                    is AppResult.Failure -> _state.update {
                        it.copy(isSearching = false, error = result.error.toUiText())
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadMore() {
        val snapshot = _state.value
        if (!snapshot.canLoadMore) return
        val nextPage = snapshot.page + 1

        viewModelScope.launch {
            _state.update { it.copy(isAppending = true) }
            val result = if (snapshot.query.isBlank()) {
                discoverMovies(snapshot.selectedGenreId, nextPage)
            } else {
                searchMovies(snapshot.query, nextPage)
            }
            _state.update { current ->
                // Guard against a race: if the query changed while this page was
                // in flight, drop the response rather than mixing result sets.
                val stale = current.query != snapshot.query ||
                    current.selectedGenreId != snapshot.selectedGenreId
                when {
                    stale -> current.copy(isAppending = false)
                    result is AppResult.Success -> current.copy(
                        isAppending = false,
                        // TMDB repeats titles across adjacent pages; dedupe by id.
                        results = (current.results + result.value.movies).distinctBy { it.id },
                        page = result.value.page,
                        totalPages = result.value.totalPages,
                    )
                    // Appending fails quietly — scrolling again retries.
                    else -> current.copy(isAppending = false)
                }
            }
        }
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                queryInput.value = intent.query
                _state.update { it.copy(query = intent.query) }
            }
            is SearchIntent.GenreSelected -> {
                genreInput.value = intent.genreId
                _state.update { it.copy(selectedGenreId = intent.genreId) }
            }
            SearchIntent.LoadMore -> loadMore()
            is SearchIntent.MovieClicked ->
                _effects.trySend(SearchEffect.NavigateToDetail(intent.movie.id))
            SearchIntent.Retry -> retries.update { it + 1 }
        }
    }
}
