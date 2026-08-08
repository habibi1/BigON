package com.bigon.sinema.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlagRepository
import com.bigon.core.config.Flags
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.DiscoverFilters
import com.bigon.domain.movie.DiscoverMoviesUseCase
import com.bigon.domain.movie.GetStreamingServicesUseCase
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
    private val getStreamingServices: GetStreamingServicesUseCase,
    flags: FeatureFlagRepository,
    tracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects: Flow<SearchEffect> = _effects.receiveAsFlow()

    private val queryInput = MutableStateFlow("")
    private val genreInput = MutableStateFlow<Int?>(null)
    private val serviceInput = MutableStateFlow<Int?>(null)
    private val filterInput = MutableStateFlow(DiscoverFilters())
    private val retries = MutableStateFlow(0)

    private val debounceMillis = flags.get(Flags.SearchDebounceMs).toLong()

    init {
        tracker.track(AnalyticsEvent.ScreenView("search"))

        observeGenres()
            .onEach { genres -> _state.update { it.copy(genres = genres) } }
            .launchIn(viewModelScope)

        // The service catalogue is region-dependent and slow-changing, so it is
        // fetched once. A failure is silent by design: no filter row is a
        // smaller loss than an error banner over working search results.
        viewModelScope.launch {
            (getStreamingServices() as? AppResult.Success)?.let { result ->
                _state.update { it.copy(services = result.value.take(MAX_SERVICES)) }
            }
        }

        queryInput
            // Clearing the field should react instantly; only typing is debounced.
            .debounce { input -> if (input.isBlank()) 0L else debounceMillis }
            .combine(genreInput) { query, genre -> query.trim() to genre }
            .combine(serviceInput) { (query, genre), service -> Triple(query, genre, service) }
            .combine(filterInput) { (query, genre, service), filters ->
                Request(query, genre, service, filters)
            }
            // Retries re-emit the same request; StateFlow inputs dedupe themselves.
            .combine(retries) { request, _ -> request }
            .mapLatest { (query, genreId, serviceId, filters) ->
                _state.update { it.copy(isSearching = true, error = null) }
                if (query.isBlank()) {
                    discoverMovies(genreId, streamingProviderId = serviceId, filters = filters)
                } else {
                    // `/search/movie` takes no provider filter and its results
                    // carry no provider data, so the service selection simply
                    // does not apply here — see SearchUiState.
                    searchMovies(query)
                }
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
                discoverMovies(
                    snapshot.selectedGenreId,
                    nextPage,
                    snapshot.selectedServiceId,
                    snapshot.filters,
                )
            } else {
                searchMovies(snapshot.query, nextPage)
            }
            _state.update { current ->
                // Guard against a race: if the query changed while this page was
                // in flight, drop the response rather than mixing result sets.
                val stale = current.query != snapshot.query ||
                    current.selectedGenreId != snapshot.selectedGenreId ||
                    current.selectedServiceId != snapshot.selectedServiceId ||
                    current.filters != snapshot.filters
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
            SearchIntent.FilterSheetOpened -> _state.update { it.copy(isFilterSheetOpen = true) }
            SearchIntent.FilterSheetDismissed -> _state.update { it.copy(isFilterSheetOpen = false) }
            is SearchIntent.FiltersChanged -> {
                filterInput.value = intent.filters
                _state.update { it.copy(filters = intent.filters) }
            }
            is SearchIntent.ServiceSelected -> {
                serviceInput.value = intent.serviceId
                _state.update { it.copy(selectedServiceId = intent.serviceId) }
            }
            SearchIntent.LoadMore -> loadMore()
            is SearchIntent.MovieClicked ->
                _effects.trySend(SearchEffect.NavigateToDetail(intent.movie.id))
            SearchIntent.Retry -> retries.update { it + 1 }
        }
    }

    /** Four inputs is past what Triple can carry legibly. */
    private data class Request(
        val query: String,
        val genreId: Int?,
        val serviceId: Int?,
        val filters: DiscoverFilters,
    )

    private companion object {
        /**
         * The US catalogue lists 289 services. Past the first handful they are
         * long-tail channels nobody filters by, and a section you have to
         * scroll for a minute to reach the end of is one nobody finishes.
         */
        const val MAX_SERVICES = 12
    }
}
