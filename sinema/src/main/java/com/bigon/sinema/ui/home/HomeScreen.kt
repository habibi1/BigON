package com.bigon.sinema.ui.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.foundation.lazy.grid.LazyGridState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.bigon.tmdb.ui.BigonShimmerCard
import com.bigon.core.designsystem.components.BigonSnackbar
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieCategory
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.bigon.core.designsystem.components.BigonLoadingIndicator
import com.bigon.core.ui.LoadMoreEffect
import com.bigon.core.ui.ObserveEffects
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.metaLine
import com.bigon.sinema.ui.posterModifier
import androidx.compose.foundation.layout.BoxScope
import com.bigon.tmdb.model.TrendingItem
import com.bigon.tmdb.model.typeLabel

/**
 * Stateful entry point: owns nothing but the ViewModel, so the stateless
 * [HomeScreen] below stays previewable and testable.
 */
@Composable
fun HomeRoute(
    onMovieClick: (Long) -> Unit,
    onTvClick: (Long) -> Unit = {},
    onPersonClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeEffect.NavigateToDetail -> onMovieClick(effect.movieId)
            is HomeEffect.NavigateToTv -> onTvClick(effect.tvId)
            is HomeEffect.NavigateToPerson -> onPersonClick(effect.personId)
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        transition = transition,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
) {
    val spacing = BigonTheme.spacing
    // One scroll position per feed, not one shared by all of them.
    //
    // A single LazyGridState meant every chip scrolled the same object: leaving
    // "Today" at row 40 and coming back put you at whatever "Popular" had left
    // behind, and the empty frame between the two lists reset it to zero. Each
    // feed keeps its own, so returning to a chip returns to where you were.
    //
    // Saved rather than merely remembered. Opening a film takes this screen out
    // of composition, and a plain remember dies with it — so every return from
    // a detail screen landed back at the first row, however far down the reader
    // had been. rememberSaveable outlives that, because the navigation entry
    // holds a destination's saveable state while it sits in the back stack.
    //
    // Hoisted rather than declared inside the grid so the chip row can reach
    // the current one — re-tapping the active chip is "take me back to the
    // top", not a reload.
    val gridStates = rememberSaveable(saver = FeedScrollPositions) { mutableMapOf() }
    val gridState = gridStates.getOrPut(state.feed.label) { LazyGridState() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = spacing.l),
    ) {
        Text(
            text = "Sinema",
            style = BigonTheme.typography.display,
            color = BigonTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.l),
        )

        // The mixed feed sits last: it is the widest net, and the movie
        // categories are what most sessions actually start from.
        val feeds = MovieCategory.entries.map { HomeFeed.Category(it) } + HomeFeed.AcrossTypes
        BigonChipRow(
            options = feeds.map { it.label },
            selectedOption = state.feed.label,
            onOptionSelect = { label ->
                feeds.firstOrNull { it.label == label }?.let { feed ->
                    // Two gestures, one job each. Tapping the chip you are
                    // already on returns you to the top; refreshing is the pull.
                    // Overloading the chip with both meant a tap that looked
                    // like navigation silently discarded every page you had
                    // scrolled.
                    if (feed == state.feed) {
                        scope.launch { gridState.animateScrollToItem(0) }
                    } else {
                        onIntent(HomeIntent.FeedSelected(feed))
                    }
                }
            },
        )

        // A failed refresh never hides content: the notice sits above whatever
        // is still cached.
        state.error?.let { error ->
            BigonSnackbar(
                message = error.asString(),
                actionLabel = "RETRY",
                onAction = { onIntent(HomeIntent.Refresh) },
                modifier = Modifier.padding(top = spacing.m),
            )
        }

        when {
            state.showSkeletons -> MovieGrid(
                itemCount = 6,
                contentPadding = PaddingValues(bottom = spacing.l),
            ) { BigonShimmerCard(width = Dp.Unspecified, modifier = Modifier.fillMaxWidth()) }

            state.showEmptyState -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BigonEmptyState(
                    icon = BigonIcons.Movie,
                    title = "Nothing to show",
                    subtitle = "Pull a category from TMDB to get started.",
                )
            }

            else -> {
                gridState.LoadMoreEffect(enabled = state.canLoadMore) {
                    onIntent(HomeIntent.LoadMore)
                }

                // A pull replaces the list from page 1, so the viewport comes
                // with it — otherwise the grid stays at its old index over a
                // much shorter list, lands near the end of it, and load more
                // fires for a page nobody asked for.
                //
                // Keyed on the pull specifically, not on isRefreshing: switching
                // feed also refreshes, and scrolling to the top there would undo
                // the position this screen just restored.
                LaunchedEffect(state.isPullRefreshing) {
                    if (state.isPullRefreshing) gridState.animateScrollToItem(0)
                }

                key(state.feed.label) {
                PullToRefreshBox(
                    isRefreshing = state.isPullRefreshing,
                    onRefresh = { onIntent(HomeIntent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.l),
                    verticalArrangement = Arrangement.spacedBy(spacing.l),
                    contentPadding = PaddingValues(bottom = spacing.l),
                    // Fixed gutter: contentPadding scrolls away with the content,
                    // which let cards run up against the chips mid-scroll.
                    modifier = Modifier.fillMaxSize().padding(top = spacing.m),
                ) {
                    if (state.isAcrossTypes) {
                        // Keyed by type as well as id: TMDB ids are unique only
                        // within a media type, so film 550 and series 550 would
                        // collide on id alone and Compose would reuse the wrong
                        // slot.
                        items(
                            state.trendingItems,
                            key = { "${it.typeLabel}-${it.id}" },
                        ) { item ->
                            TrendingCard(
                                item = item,
                                transition = transition,
                                onClick = { onIntent(HomeIntent.TrendingItemClicked(item)) },
                            )
                        }
                    } else {
                        items(state.movies, key = { it.id }) { movie ->
                            MovieCard(
                                movie = movie,
                                transition = transition,
                                onClick = { onIntent(HomeIntent.MovieClicked(movie)) },
                            )
                        }
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
    }
}

@Composable
private fun MovieCard(movie: Movie, transition: PosterTransition?, onClick: () -> Unit) {
    BigonMovieCard(
        title = movie.title,
        meta = movie.metaLine(),
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
                    // Same key as the detail poster: this is the element that flies.
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transition.posterModifier(movie.id)),
                )
            }
        },
    )
}

/**
 * Scroll position per feed, flattened to `label, index, offset` triples so it
 * survives in a Bundle.
 *
 * Positions rather than the states themselves: a LazyGridState is not
 * parcelable, and the two numbers are the whole of what the reader would miss.
 */
private val FeedScrollPositions = listSaver<MutableMap<String, LazyGridState>, Any>(
    save = { states ->
        states.flatMap { (label, state) ->
            listOf(label, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }
    },
    restore = { flat ->
        flat.chunked(3).associate { (label, index, offset) ->
            label as String to LazyGridState(index as Int, offset as Int)
        }.toMutableMap()
    },
)

@Composable
private fun MovieGrid(
    itemCount: Int,
    contentPadding: PaddingValues,
    item: @Composable () -> Unit,
) {
    val spacing = BigonTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize().padding(top = spacing.m),
    ) {
        items(List(itemCount) { it }) { item() }
    }
}

/**
 * One entry of the mixed feed. A film reuses the existing card and keeps its
 * shared-element transition into detail; a series or person uses the same shape
 * without one, because they have no detail screen to fly into.
 *
 * The meta line always names the type. In a list that interleaves three kinds
 * of thing, "2024 · Drama" and "Series · 2024" have to be told apart at a
 * glance, and a poster alone does not do it.
 */
@Composable
private fun TrendingCard(
    item: TrendingItem,
    transition: PosterTransition?,
    onClick: () -> Unit,
) {
    when (item) {
        is TrendingItem.Film -> MovieCard(movie = item.movie, transition = transition, onClick = onClick)

        is TrendingItem.Series -> BigonMovieCard(
            title = item.name,
            meta = listOfNotNull("Series", item.firstAirYear?.toString()).joinToString(" · "),
            rating = item.voteAverage,
            onClick = onClick,
            width = Dp.Unspecified,
            modifier = Modifier.fillMaxWidth(),
            poster = { PosterOrPlaceholder(item.posterUrl) },
        )

        is TrendingItem.Person -> BigonMovieCard(
            title = item.name,
            meta = listOfNotNull("Person", item.knownForDepartment).joinToString(" · "),
            onClick = onClick,
            width = Dp.Unspecified,
            modifier = Modifier.fillMaxWidth(),
            poster = { PosterOrPlaceholder(item.profileUrl) },
        )
    }
}

@Composable
private fun BoxScope.PosterOrPlaceholder(url: String?) {
    url?.let {
        AsyncImage(
            model = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } ?: BigonPosterPlaceholder()
}
