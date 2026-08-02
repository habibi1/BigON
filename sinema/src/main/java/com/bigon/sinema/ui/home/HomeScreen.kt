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
import com.bigon.core.designsystem.components.SinemaShimmerCard
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.model.Movie
import com.bigon.core.model.MovieCategory
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.ui.LoadMoreEffect
import com.bigon.core.ui.ObserveEffects
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.posterModifier

/**
 * Stateful entry point: owns nothing but the ViewModel, so the stateless
 * [HomeScreen] below stays previewable and testable.
 */
@Composable
fun HomeRoute(
    onMovieClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeEffect.NavigateToDetail -> onMovieClick(effect.movieId)
        }
    }

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        transition = transition,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
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
        Text(
            text = "Sinema",
            style = SinemaTheme.typography.display,
            color = SinemaTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.l),
        )

        SinemaChipRow(
            options = MovieCategory.entries.map { it.label },
            selectedOption = state.category.label,
            onOptionSelect = { label ->
                MovieCategory.entries.firstOrNull { it.label == label }
                    ?.let { onIntent(HomeIntent.CategorySelected(it)) }
            },
        )

        // A failed refresh never hides content: the notice sits above whatever
        // is still cached.
        state.error?.let { error ->
            SinemaSnackbar(
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
            ) { SinemaShimmerCard(width = Dp.Unspecified, modifier = Modifier.fillMaxWidth()) }

            state.showEmptyState -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SinemaEmptyState(
                    icon = SinemaIcons.Movie,
                    title = "Nothing to show",
                    subtitle = "Pull a category from TMDB to get started.",
                )
            }

            else -> {
                val gridState = rememberLazyGridState()
                gridState.LoadMoreEffect(enabled = state.canLoadMore) {
                    onIntent(HomeIntent.LoadMore)
                }
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
                    items(state.movies, key = { it.id }) { movie ->
                        MovieCard(
                            movie = movie,
                            transition = transition,
                            onClick = { onIntent(HomeIntent.MovieClicked(movie)) },
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

@Composable
private fun MovieCard(movie: Movie, transition: PosterTransition?, onClick: () -> Unit) {
    SinemaMovieCard(
        title = movie.title,
        meta = movie.metaLine(),
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
                    // Same key as the detail poster: this is the element that flies.
                    modifier = Modifier
                        .fillMaxSize()
                        .then(transition.posterModifier(movie.id)),
                )
            }
        },
    )
}

/** "2026 · Sci-Fi", degrading gracefully when either half is missing. */
private fun Movie.metaLine(): String? =
    listOfNotNull(releaseYear?.toString(), genres.firstOrNull())
        .joinToString(" · ")
        .takeIf { it.isNotBlank() }

@Composable
private fun MovieGrid(
    itemCount: Int,
    contentPadding: PaddingValues,
    item: @Composable () -> Unit,
) {
    val spacing = SinemaTheme.spacing
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
