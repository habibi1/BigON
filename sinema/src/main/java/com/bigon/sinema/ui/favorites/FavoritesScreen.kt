package com.bigon.sinema.ui.favorites

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
import com.bigon.core.designsystem.components.BigonEmptyState
import com.bigon.tmdb.ui.BigonMovieCard
import com.bigon.tmdb.ui.BigonPosterPlaceholder
import com.bigon.core.designsystem.components.BigonTonalButton
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.tmdb.model.Movie
import com.bigon.core.ui.ObserveEffects
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.posterModifier

@Composable
fun FavoritesRoute(
    onMovieClick: (Long) -> Unit,
    onBrowseTrending: () -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is FavoritesEffect.NavigateToDetail -> onMovieClick(effect.movieId)
            FavoritesEffect.NavigateToHome -> onBrowseTrending()
        }
    }

    FavoritesScreen(
        state = state,
        onIntent = viewModel::onIntent,
        transition = transition,
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onIntent: (FavoritesIntent) -> Unit,
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
        Text(
            text = "Favorites",
            style = BigonTheme.typography.display,
            color = BigonTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.l),
        )

        if (state.showEmptyState) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BigonEmptyState(
                    icon = BigonIcons.HeartOutline,
                    title = "No favorites yet",
                    subtitle = "Movies you favorite appear here and work offline.",
                    action = {
                        BigonTonalButton(
                            text = "Browse trending",
                            onClick = { onIntent(FavoritesIntent.BrowseTrending) },
                        )
                    },
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.l),
                verticalArrangement = Arrangement.spacedBy(spacing.l),
                contentPadding = PaddingValues(bottom = spacing.l),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.favorites, key = { it.id }) { movie ->
                    FavoriteCard(
                        movie = movie,
                        transition = transition,
                        onClick = { onIntent(FavoritesIntent.MovieClicked(movie)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(movie: Movie, transition: PosterTransition?, onClick: () -> Unit) {
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
