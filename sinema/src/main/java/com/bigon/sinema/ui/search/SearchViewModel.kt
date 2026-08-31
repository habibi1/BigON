package com.bigon.sinema.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.config.FeatureFlagRepository
import com.bigon.core.config.Flags
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.toUiText
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MoviePage
import com.bigon.tmdb.domain.movie.DiscoverFilters
import com.bigon.tmdb.domain.movie.DiscoverMoviesUseCase
import com.bigon.tmdb.domain.movie.GetStreamingServicesUseCase
import com.bigon.tmdb.domain.movie.ObserveGenresUseCase
import com.bigon.tmdb.domain.movie.SearchMoviesUseCase
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
import kotlinx.coroutines.flow.updateAndGet
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

    // Declared above `init` deliberately. Kotlin runs property initialisers and
    // init blocks in source order, and the pipeline in `init` reaches this map
    // on its very first emission — which happens during construction. Declared
    // below, it is still null at that point and the screen crashes on open.
    /**
     * One list of results per selection, so a chip returned to is the chip that
     * was left rather than a fresh page 1.
     *
     * Deliberately in memory and deliberately not in Room: these are transient
     * views of TMDB's catalogue — a genre page, a query's hits — and writing
     * them to the tables that back Home would mix them into content the app
     * treats as its own cache. They live as long as the screen does.
     *
     * The trade is staleness: a list returned to inside one session is the list
     * that was fetched, not what TMDB holds now. For a film catalogue that is
     * invisible, and it costs a request rather than making one.
     */
    private data class RememberedList(
        val movies: List<Movie>,
        val page: Int,
        val totalPages: Int,
    )

    /** Insertion-ordered, so the first key is the least recently stored. */
    private val remembered = LinkedHashMap<String, RememberedList>()

    private fun keep(key: String, list: RememberedList) {
        // Re-inserting moves a refreshed list to the newest end, so appending to
        // a list cannot get it evicted while it is the one being read.
        remembered.remove(key)
        if (remembered.size >= MAX_REMEMBERED_LISTS) {
            remembered.remove(remembered.keys.first())
        }
        remembered[key] = list
    }


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
            .mapLatest { request ->
                val key = SearchUiState.selectionKey(
                    request.query,
                    request.genreId,
                    request.serviceId,
                    request.filters,
                )

                // A list already fetched comes back exactly as it was left,
                // every appended page included. Refetching would return page 1
                // and silently shorten the list, which is what used to make a
                // remembered scroll position meaningless.
                //
                // Failures are never stored, so a retry finds nothing here and
                // goes to the network as it should.
                remembered[key]?.let { return@mapLatest Restored(key, it) }

                // The previous selection's films are not this selection's, so
                // they go the moment the request starts rather than sitting
                // there until it lands. Leaving them up meant a chip change
                // showed the old chip's films first and swapped them a moment
                // later, which reads as the wrong list having been opened.
                //
                // Clearing here, past the cache check, keeps a remembered
                // selection off this path: that one swaps straight to its own
                // films with no skeleton in between.
                _state.update {
                    it.copy(
                        isSearching = true,
                        error = null,
                        results = emptyList(),
                        page = 1,
                        totalPages = 1,
                    )
                }
                val result = if (request.query.isBlank()) {
                    discoverMovies(
                        request.genreId,
                        streamingProviderId = request.serviceId,
                        filters = request.filters,
                    )
                } else {
                    // `/search/movie` takes no provider filter and its results
                    // carry no provider data, so the service selection simply
                    // does not apply here — see SearchUiState.
                    searchMovies(request.query)
                }
                Fetched(key, result)
            }
            .onEach { outcome ->
                when (outcome) {
                    is Restored -> _state.update {
                        it.copy(
                            isSearching = false,
                            error = null,
                            results = outcome.results.movies,
                            page = outcome.results.page,
                            totalPages = outcome.results.totalPages,
                        )
                    }

                    is Fetched -> when (val result = outcome.result) {
                        // A new request always replaces: page 1 of a different query.
                        is AppResult.Success -> {
                            keep(
                                outcome.key,
                                RememberedList(
                                    movies = result.value.movies,
                                    page = result.value.page,
                                    totalPages = result.value.totalPages,
                                ),
                            )
                            _state.update {
                                it.copy(
                                    isSearching = false,
                                    results = result.value.movies,
                                    page = result.value.page,
                                    totalPages = result.value.totalPages,
                                )
                            }
                        }

                        is AppResult.Failure -> _state.update {
                            it.copy(isSearching = false, error = result.error.toUiText())
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadMore() {
        val snapshot = _state.value
        if (!snapshot.canLoadMore) return
        val nextPage = snapshot.page + 1

        val snapshotKey = snapshot.selectionKey

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
            val updated = _state.updateAndGet { current ->
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

            // The remembered copy has to grow with the visible one. Without this
            // a list is stored at page 1 forever, and returning to it would
            // rewind past everything the reader had scrolled through — the same
            // shortening as a refetch, just arriving from the cache instead.
            // Skipped when the update above judged the response stale.
            if (result is AppResult.Success && updated.selectionKey == snapshotKey) {
                keep(
                    snapshotKey,
                    RememberedList(updated.results, updated.page, updated.totalPages),
                )
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

    /** What one turn of the pipeline produced: a stored list, or a request. */
    private sealed interface Outcome

    private data class Restored(val key: String, val results: RememberedList) : Outcome

    private data class Fetched(val key: String, val result: AppResult<MoviePage>) : Outcome

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
         * long-tail channels nobody filters by, and an endless row would bury
         * the genre chips beneath it.
         */
        const val MAX_SERVICES = 12

        /**
         * Every keystroke is its own list, so this is bounded by what a reader
         * plausibly returns to rather than by what they can generate. Being
         * evicted costs one refetch, which is what every selection cost before.
         */
        const val MAX_REMEMBERED_LISTS = 16
    }
}
