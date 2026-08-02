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
import com.bigon.core.designsystem.components.SinemaChipRow
import com.bigon.core.designsystem.components.SinemaEmptyState
import com.bigon.core.designsystem.components.SinemaMovieCard
import com.bigon.core.designsystem.components.SinemaPosterPlaceholder
import com.bigon.core.designsystem.components.SinemaSearchBar
import com.bigon.core.designsystem.components.SinemaShimmerCard
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.model.Movie
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.ui.LoadMoreEffect
import com.bigon.core.ui.ObserveEffects
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.posterModifier

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
    val spacing = SinemaTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = spacing.l),
    ) {
        SinemaSearchBar(
            query = state.query,
            onQueryChange = { onIntent(SearchIntent.QueryChanged(it)) },
            modifier = Modifier.padding(top = spacing.l),
        )

        if (state.genres.isNotEmpty()) {
            SinemaChipRow(
                options = listOf(ALL_CHIP) + state.genres.map { it.name },
                selectedOption = state.selectedGenreName ?: ALL_CHIP,
                onOptionSelect = { label ->
                    val genreId = state.genres.firstOrNull { it.name == label }?.id
                    onIntent(SearchIntent.GenreSelected(genreId))
                },
                modifier = Modifier.padding(top = spacing.m),
            )
        }

        state.error?.let { error ->
            SinemaSnackbar(
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
                                SinemaLoadingIndicator()
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
    val spacing = SinemaTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        contentPadding = PaddingValues(bottom = spacing.l),
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
