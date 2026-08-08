package com.bigon.sinema.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.core.designsystem.components.SinemaEmptyState
import com.bigon.core.designsystem.components.SinemaFilterChip
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.designsystem.components.SinemaMovieCard
import com.bigon.core.designsystem.components.SinemaPosterPlaceholder
import com.bigon.core.designsystem.components.SinemaPrimaryButton
import com.bigon.core.designsystem.components.SinemaSearchBar
import com.bigon.core.designsystem.components.SinemaShimmerCard
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.model.Genre
import com.bigon.core.model.Movie
import com.bigon.core.model.WatchProvider
import com.bigon.core.ui.LoadMoreEffect
import com.bigon.core.ui.ObserveEffects
import com.bigon.core.ui.asString
import com.bigon.domain.movie.DiscoverFilters
import com.bigon.domain.movie.DiscoverSort
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.posterModifier
import kotlinx.coroutines.launch

private const val ALL_GENRES = "All"
private const val ANY_OPTION = "Any"

@Composable
fun SearchRoute(
    onMovieClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is SearchEffect.NavigateToDetail -> onMovieClick(effect.movieId)
        }
    }

    SearchScreen(
        state = state,
        onIntent = viewModel::onIntent,
        transition = transition,
        modifier = modifier,
    )
}

/**
 * Chrome above the results is one search field and one filter bar.
 *
 * It used to be four stacked rows — search, genres, a "Refine" chip beside a
 * bare "Clear" link, and a row of unlabelled provider logos — roughly 120dp of
 * controls between the search field and the first poster on a phone. Every one
 * of them was a filter, so none of them explained the others, and the screen
 * led with its settings rather than its content.
 *
 * Now genre stays on the surface, because it is the axis people actually reach
 * for, and everything else lives behind one badged entry point. The count on
 * that chip is what keeps the collapse honest: refinements can be out of sight
 * without being out of mind.
 *
 * The horizontal padding is deliberately not on this Column. Scrolling rows
 * need to bleed to the screen edge and inset their *content* instead, or chips
 * clip against a parent's padding mid-scroll and read as broken rather than
 * scrollable.
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
) {
    val spacing = SinemaTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        SinemaSearchBar(
            query = state.query,
            onQueryChange = { onIntent(SearchIntent.QueryChanged(it)) },
            modifier = Modifier
                .padding(horizontal = spacing.l)
                .padding(top = spacing.l),
        )

        SearchFilterBar(
            genres = state.genres,
            selectedGenreId = state.selectedGenreId,
            showFilterButton = state.showFilters,
            activeCount = state.activeRefinementCount,
            onGenreSelect = { onIntent(SearchIntent.GenreSelected(it)) },
            onOpenFilters = { onIntent(SearchIntent.FilterSheetOpened) },
            modifier = Modifier.padding(top = spacing.s),
        )

        if (state.isFilterSheetOpen) {
            FiltersSheet(
                filters = state.filters,
                services = state.services,
                selectedServiceId = state.selectedServiceId,
                showServices = state.showServiceFilter,
                // Both inputs are StateFlows on the ViewModel, so an unchanged
                // one dedupes itself and a changed one cancels the request the
                // other started — closing the sheet costs one search, not two.
                onDismiss = { filters, serviceId ->
                    onIntent(SearchIntent.FiltersChanged(filters))
                    onIntent(SearchIntent.ServiceSelected(serviceId))
                    onIntent(SearchIntent.FilterSheetDismissed)
                },
            )
        }

        state.error?.let { error ->
            SinemaSnackbar(
                message = error.asString(),
                actionLabel = "RETRY",
                onAction = { onIntent(SearchIntent.Retry) },
                modifier = Modifier.padding(horizontal = spacing.l, vertical = spacing.m),
            )
        }

        when {
            state.showSkeletons -> ResultGrid()

            state.showEmptyState -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.l),
                contentAlignment = Alignment.Center,
            ) {
                SinemaEmptyState(
                    icon = SinemaIcons.Search,
                    title = "No results",
                    subtitle = if (state.query.isBlank()) {
                        "Nothing to browse right now — try again."
                    } else {
                        "Nothing matches \"${state.query}\"${state.selectedGenreName?.let { " in $it" } ?: ""}."
                    },
                )
            }

            else -> {
                val gridState = rememberLazyGridState()
                gridState.LoadMoreEffect(enabled = state.canLoadMore) {
                    onIntent(SearchIntent.LoadMore)
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.l),
                    verticalArrangement = Arrangement.spacedBy(spacing.l),
                    contentPadding = PaddingValues(
                        start = spacing.l,
                        end = spacing.l,
                        bottom = spacing.l,
                    ),
                    // Fixed gutter so results never touch the filter bar mid-scroll.
                    modifier = Modifier.fillMaxSize().padding(top = spacing.m),
                ) {
                    items(state.visibleResults, key = { it.id }) { movie ->
                        ResultCard(
                            movie = movie,
                            transition = transition,
                            onClick = { onIntent(SearchIntent.MovieClicked(movie)) },
                        )
                    }
                    if (state.isAppending) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(spacing.l),
                                contentAlignment = Alignment.Center,
                            ) {
                                SinemaLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The filter bar: a pinned entry point, a rule, then genres scrolling past it.
 *
 * The entry point is pinned rather than scrolled with the chips because it is
 * the one control on the row that does not belong to the genre axis — letting
 * it slide away behind "Documentary" would hide the only way back to
 * everything else. The rule is what stops the two reading as one long list of
 * peers.
 */
@Composable
private fun SearchFilterBar(
    genres: List<Genre>,
    selectedGenreId: Int?,
    showFilterButton: Boolean,
    activeCount: Int,
    onGenreSelect: (Int?) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showFilterButton && genres.isEmpty()) return

    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (showFilterButton) {
            SinemaFilterChip(
                label = "Filters",
                selected = activeCount > 0,
                count = activeCount,
                onClick = onOpenFilters,
                leading = {
                    Icon(
                        imageVector = SinemaIcons.Filter,
                        contentDescription = null,
                        tint = if (activeCount > 0) colors.onPrimaryContainer else colors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                },
                modifier = Modifier.padding(start = spacing.l),
            )
            if (genres.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = spacing.m)
                        .size(width = 1.dp, height = 20.dp)
                        .background(colors.outline),
                )
            }
        }

        if (genres.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                contentPadding = PaddingValues(
                    start = if (showFilterButton) 0.dp else spacing.l,
                    end = spacing.l,
                ),
            ) {
                item(key = ALL_GENRES) {
                    SinemaFilterChip(
                        label = ALL_GENRES,
                        selected = selectedGenreId == null,
                        onClick = { onGenreSelect(null) },
                    )
                }
                items(genres, key = { it.id }) { genre ->
                    SinemaFilterChip(
                        label = genre.name,
                        selected = genre.id == selectedGenreId,
                        // Tapping the active genre falls back to All, which is
                        // the same gesture the All chip performs — either way
                        // out works, and neither is the only one.
                        onClick = { onGenreSelect(genre.id.takeIf { it != selectedGenreId }) },
                    )
                }
            }
        }
    }
}

/** Skeleton grid shown only while there is nothing at all to display. */
@Composable
private fun ResultGrid() {
    val spacing = SinemaTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        contentPadding = PaddingValues(start = spacing.l, end = spacing.l, bottom = spacing.l),
        modifier = Modifier.fillMaxSize().padding(top = spacing.m),
    ) {
        items(List(6) { it }) {
            SinemaShimmerCard(width = Dp.Unspecified, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultCard(movie: Movie, transition: PosterTransition?, onClick: () -> Unit) {
    SinemaMovieCard(
        title = movie.title,
        meta = listOfNotNull(movie.releaseYear?.toString(), movie.genres.firstOrNull())
            .joinToString(" · ")
            .takeIf { it.isNotBlank() },
        rating = movie.voteAverage,
        onClick = onClick,
        width = Dp.Unspecified,
        modifier = Modifier.fillMaxWidth(),
        poster = {
            if (movie.posterUrl == null) {
                SinemaPosterPlaceholder()
            } else {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transition.posterModifier(movie.id)),
                )
            }
        },
    )
}

/**
 * A provider mark on a fixed light plate.
 *
 * TMDB ships these as dark-on-transparent PNGs, so on the dark theme's
 * surface a mark like Netflix's wordmark is very nearly invisible — the same
 * defect the TMDB attribution logo had at 1.14:1 (§6), and the reason the
 * plate is a literal white rather than a theme colour: it is backing someone
 * else's brand, which does not restyle with ours.
 *
 * The name is rendered as the chip's label beside this, so the image itself is
 * decorative and carries no content description — announcing it would read the
 * service twice.
 */
@Composable
private fun ServiceLogo(service: WatchProvider) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(SinemaTheme.shapes.badge)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        service.logoUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The filter sheet.
 *
 * A bottom sheet rather than the centre dialog this used to be: filters are a
 * secondary surface over results you are still meant to feel present, and a
 * dialog dims and detaches the whole screen to say the same thing a sheet says
 * by sliding up from the thumb.
 *
 * Deliberately five sections, not the thirty TMDB's discover endpoint accepts.
 * The rest — language, cast, keyword, company — are either already covered by
 * another affordance on this screen or are the kind of filter people reach for
 * once a year, and every one added costs a row that everyone scrolls past.
 *
 * Selections are held as a local draft and committed once, on the way out.
 * Applying each tap live would mean a discover request per refinement, against
 * results sitting behind the sheet where nobody can see them. Every exit
 * commits — the button, the scrim, the drag, the back gesture — because a
 * sheet where only one particular button keeps your choices is a sheet that
 * loses them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(
    filters: DiscoverFilters,
    services: List<WatchProvider>,
    selectedServiceId: Int?,
    showServices: Boolean,
    onDismiss: (DiscoverFilters, Int?) -> Unit,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors
    val thisYear = remember { java.time.LocalDate.now().year }
    var draft by remember(filters) { mutableStateOf(filters) }
    var draftService by remember(selectedServiceId) { mutableStateOf(selectedServiceId) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commit = { onDismiss(draft, draftService) }
    // The scrim, the drag and the back gesture have already played the exit
    // animation by the time they call back; the button has not, so it asks for
    // one rather than making its own dismissal the abrupt way out.
    val commitAfterHiding = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) commit()
        }
        Unit
    }

    ModalBottomSheet(
        onDismissRequest = commit,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.outline) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.l),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filters",
                    style = SinemaTheme.typography.title,
                    color = colors.textPrimary,
                )
                // One reset for everything the sheet owns, shown only when
                // there is something to reset. The old screen put a bare
                // "Clear" link next to the chip that opened this, where it was
                // ambiguous about whether genre went with it.
                if (draft.isActive || draftService != null) {
                    Text(
                        text = "Reset",
                        style = SinemaTheme.typography.label,
                        color = colors.primary,
                        modifier = Modifier
                            .clip(SinemaTheme.shapes.pill)
                            .clickable {
                                draft = DiscoverFilters()
                                draftService = null
                            }
                            .padding(horizontal = spacing.m, vertical = spacing.m),
                    )
                }
            }

            // The sections scroll; the commit button below does not, so the way
            // out is reachable without scrolling back to find it.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (showServices) {
                    FilterSectionLabel("Streaming service")
                    FilterOptionRow {
                        SinemaFilterChip(
                            label = ANY_OPTION,
                            selected = draftService == null,
                            onClick = { draftService = null },
                        )
                        services.forEach { service ->
                            SinemaFilterChip(
                                label = service.name,
                                selected = service.id == draftService,
                                onClick = { draftService = service.id.takeIf { it != draftService } },
                                leading = { ServiceLogo(service) },
                            )
                        }
                    }
                }

                FilterSectionLabel("Sort by")
                FilterOptionRow {
                    DiscoverSort.entries.forEach { sort ->
                        SinemaFilterChip(
                            label = sort.label,
                            selected = draft.sort == sort,
                            onClick = { draft = draft.copy(sort = sort) },
                        )
                    }
                }

                FilterSectionLabel("Release year")
                FilterOptionRow {
                    SinemaFilterChip(
                        label = ANY_OPTION,
                        selected = draft.releaseYear == null,
                        onClick = { draft = draft.copy(releaseYear = null) },
                    )
                    (0..7).map { thisYear - it }.forEach { year ->
                        SinemaFilterChip(
                            label = year.toString(),
                            selected = draft.releaseYear == year,
                            onClick = { draft = draft.copy(releaseYear = year) },
                        )
                    }
                }

                FilterSectionLabel("Minimum rating")
                FilterOptionRow {
                    SinemaFilterChip(
                        label = ANY_OPTION,
                        selected = draft.minRating == null,
                        onClick = { draft = draft.copy(minRating = null) },
                    )
                    listOf(6.0, 7.0, 8.0).forEach { rating ->
                        SinemaFilterChip(
                            label = "★ $rating+",
                            selected = draft.minRating == rating,
                            onClick = { draft = draft.copy(minRating = rating) },
                        )
                    }
                }

                FilterSectionLabel("Maximum runtime")
                FilterOptionRow {
                    SinemaFilterChip(
                        label = ANY_OPTION,
                        selected = draft.maxRuntimeMinutes == null,
                        onClick = { draft = draft.copy(maxRuntimeMinutes = null) },
                    )
                    listOf(90, 120, 150).forEach { minutes ->
                        SinemaFilterChip(
                            label = "under ${minutes / 60}h${(minutes % 60).takeIf { it > 0 } ?: ""}",
                            selected = draft.maxRuntimeMinutes == minutes,
                            onClick = { draft = draft.copy(maxRuntimeMinutes = minutes) },
                        )
                    }
                }
            }

            SinemaPrimaryButton(
                text = "Show results",
                onClick = commitAfterHiding,
                modifier = Modifier
                    .padding(top = spacing.l, bottom = spacing.xxl)
                    .fillMaxWidth(),
            )
        }
    }
}

/** One scrolling row of options — every section in the sheet is one of these. */
@Composable
private fun FilterOptionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        content = content,
    )
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = SinemaTheme.typography.caption,
        color = SinemaTheme.colors.textSecondary,
        modifier = Modifier.padding(top = SinemaTheme.spacing.l, bottom = SinemaTheme.spacing.s),
    )
}
