package com.bigon.sinema.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.core.designsystem.components.BigonChipRow
import com.bigon.core.designsystem.components.BigonEmptyState
import com.bigon.tmdb.ui.BigonMovieCard
import com.bigon.tmdb.ui.BigonPosterPlaceholder
import com.bigon.core.designsystem.components.BigonSearchBar
import com.bigon.tmdb.ui.BigonShimmerCard
import com.bigon.core.designsystem.components.BigonSnackbar
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.tmdb.model.Movie
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.bigon.core.designsystem.components.BigonLoadingIndicator
import com.bigon.core.ui.LoadMoreEffect
import com.bigon.core.ui.ObserveEffects
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.posterModifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import com.bigon.tmdb.model.WatchProvider
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.bigon.core.designsystem.components.BigonChip
import com.bigon.core.designsystem.components.BigonPrimaryButton
import com.bigon.tmdb.domain.movie.DiscoverFilters
import com.bigon.tmdb.domain.movie.DiscoverSort

private const val ALL_CHIP = "All"

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

@Composable
fun SearchScreen(
    state: SearchUiState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
) {
    val spacing = BigonTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = spacing.l),
    ) {
        BigonSearchBar(
            query = state.query,
            onQueryChange = { onIntent(SearchIntent.QueryChanged(it)) },
            modifier = Modifier.padding(top = spacing.l),
        )

        if (state.genres.isNotEmpty()) {
            BigonChipRow(
                options = listOf(ALL_CHIP) + state.genres.map { it.name },
                selectedOption = state.selectedGenreName ?: ALL_CHIP,
                onOptionSelect = { label ->
                    val genreId = state.genres.firstOrNull { it.name == label }?.id
                    onIntent(SearchIntent.GenreSelected(genreId))
                },
                modifier = Modifier.padding(top = spacing.m),
            )
        }

        if (state.showFilters) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = spacing.s),
            ) {
                BigonChip(
                    label = if (state.filters.activeCount > 0) {
                        "Refine (${state.filters.activeCount})"
                    } else {
                        "Refine"
                    },
                    selected = state.filters.isActive,
                    onClick = { onIntent(SearchIntent.FilterSheetOpened) },
                )
                if (state.filters.isActive) {
                    Text(
                        text = "Clear",
                        style = BigonTheme.typography.caption,
                        color = BigonTheme.colors.primary,
                        modifier = Modifier
                            .padding(start = spacing.m)
                            .clip(BigonTheme.shapes.pill)
                            .clickable { onIntent(SearchIntent.FiltersChanged(DiscoverFilters())) }
                            .padding(spacing.xs),
                    )
                }
            }
        }

        // Streaming services, shown only while browsing — a typed search cannot
        // honour this filter, and a control that silently does nothing is worse
        // than one that is absent.
        if (state.showServiceFilter) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = spacing.s),
            ) {
                state.services.forEach { service ->
                    val selected = service.id == state.selectedServiceId
                    ServiceChip(
                        service = service,
                        selected = selected,
                        // Tapping the active service clears it, so the row needs
                        // no separate "All" affordance competing with the genre
                        // row's own.
                        onClick = {
                            onIntent(SearchIntent.ServiceSelected(service.id.takeIf { !selected }))
                        },
                    )
                }
            }
        }

        if (state.isFilterSheetOpen) {
            RefineSheet(
                filters = state.filters,
                onDone = {
                    onIntent(SearchIntent.FiltersChanged(it))
                    onIntent(SearchIntent.FilterSheetDismissed)
                },
            )
        }

        state.error?.let { error ->
            BigonSnackbar(
                message = error.asString(),
                actionLabel = "RETRY",
                onAction = { onIntent(SearchIntent.Retry) },
                modifier = Modifier.padding(top = spacing.m),
            )
        }

        when {
            state.showSkeletons -> ResultGrid()

            state.showEmptyState -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BigonEmptyState(
                    icon = BigonIcons.Search,
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
                    contentPadding = PaddingValues(bottom = spacing.l),
                    // Fixed gutter so results never touch the chip row mid-scroll.
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
                                BigonLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Skeleton grid shown only while there is nothing at all to display. */
@Composable
private fun ResultGrid() {
    val spacing = BigonTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        contentPadding = PaddingValues(bottom = spacing.l),
        modifier = Modifier.fillMaxSize().padding(top = spacing.m),
    ) {
        items(List(6) { it }) {
            BigonShimmerCard(width = Dp.Unspecified, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultCard(movie: Movie, transition: PosterTransition?, onClick: () -> Unit) {
    BigonMovieCard(
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
                BigonPosterPlaceholder()
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
 * A streaming service as a logo pill. The logo alone identifies the service
 * better than its name would at this size — these are among the most
 * recognisable marks on a phone — so the name is carried as the content
 * description rather than rendered.
 */
@Composable
private fun ServiceChip(
    service: WatchProvider,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = BigonTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(BigonTheme.shapes.container)
            .background(if (selected) colors.primaryContainer else colors.surfaceVariant)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) colors.primary else Color.Transparent,
                shape = BigonTheme.shapes.container,
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        service.logoUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = service.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().clip(BigonTheme.shapes.container),
            )
        } ?: Text(
            text = service.name.take(2),
            style = BigonTheme.typography.caption,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * The refine sheet.
 *
 * Deliberately four controls, not the thirty TMDB's discover endpoint accepts.
 * The rest — language, cast, keyword, company — are either already covered by
 * another affordance on this screen or are the kind of filter people reach for
 * once a year, and every one added costs a row that everyone scrolls past.
 *
 * Selections are held as a local draft and committed once, on the way out.
 * Applying each tap live would mean four discover requests to set four
 * refinements, and the results are behind the sheet where nobody can see them
 * anyway.
 */
@Composable
private fun RefineSheet(
    filters: DiscoverFilters,
    onDone: (DiscoverFilters) -> Unit,
) {
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors
    val thisYear = remember { java.time.LocalDate.now().year }
    var draft by remember(filters) { mutableStateOf(filters) }
    val commit = { onDone(draft) }

    // Back press and a scrim tap commit too: a sheet whose only exit that keeps
    // your choices is one particular button is a sheet that loses them.
    Dialog(onDismissRequest = commit) {
        Surface(shape = BigonTheme.shapes.container, color = colors.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.l),
            ) {
                Text("Refine", style = BigonTheme.typography.title, color = colors.textPrimary)

                RefineLabel("Sort by")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    DiscoverSort.entries.forEach { sort ->
                        BigonChip(
                            label = sort.label,
                            selected = draft.sort == sort,
                            onClick = { draft = draft.copy(sort = sort) },
                        )
                    }
                }

                RefineLabel("Release year")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    BigonChip(
                        label = "Any",
                        selected = draft.releaseYear == null,
                        onClick = { draft = draft.copy(releaseYear = null) },
                    )
                    (0..7).map { thisYear - it }.forEach { year ->
                        BigonChip(
                            label = year.toString(),
                            selected = draft.releaseYear == year,
                            onClick = { draft = draft.copy(releaseYear = year) },
                        )
                    }
                }

                RefineLabel("Minimum rating")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    BigonChip(
                        label = "Any",
                        selected = draft.minRating == null,
                        onClick = { draft = draft.copy(minRating = null) },
                    )
                    listOf(6.0, 7.0, 8.0).forEach { rating ->
                        BigonChip(
                            label = "★ $rating+",
                            selected = draft.minRating == rating,
                            onClick = { draft = draft.copy(minRating = rating) },
                        )
                    }
                }

                RefineLabel("Maximum runtime")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    BigonChip(
                        label = "Any",
                        selected = draft.maxRuntimeMinutes == null,
                        onClick = { draft = draft.copy(maxRuntimeMinutes = null) },
                    )
                    listOf(90, 120, 150).forEach { minutes ->
                        BigonChip(
                            label = "under ${minutes / 60}h${(minutes % 60).takeIf { it > 0 } ?: ""}",
                            selected = draft.maxRuntimeMinutes == minutes,
                            onClick = { draft = draft.copy(maxRuntimeMinutes = minutes) },
                        )
                    }
                }

                BigonPrimaryButton(
                    text = "Done",
                    onClick = commit,
                    modifier = Modifier.padding(top = spacing.xl).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RefineLabel(text: String) {
    Text(
        text = text,
        style = BigonTheme.typography.caption,
        color = BigonTheme.colors.textSecondary,
        modifier = Modifier.padding(top = BigonTheme.spacing.l, bottom = BigonTheme.spacing.s),
    )
}
