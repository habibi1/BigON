package com.bigon.sinema.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.tmdb.ui.SinemaMovieCard
import com.bigon.tmdb.ui.SinemaPosterPlaceholder
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.metaLine

@Composable
fun CollectionRoute(
    collectionId: Long,
    onBack: () -> Unit,
    onMovieClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = hiltViewModel<CollectionViewModel, CollectionViewModel.Factory>(
        key = "collection-$collectionId",
    ) { factory -> factory.create(collectionId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
            contentPadding = PaddingValues(spacing.l),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // Clears the floating back button: it ends 52dp below the
                // status bar, and the grid's own 16dp inset only gets us part
                // of the way there.
                Column(modifier = Modifier.padding(top = spacing.xxl + spacing.l)) {
                    Text(
                        text = state.collection?.name.orEmpty(),
                        style = SinemaTheme.typography.title,
                        color = colors.textPrimary,
                    )
                    state.collection?.let { collection ->
                        Text(
                            // The count is the point of a franchise view: it
                            // answers "how many of these are there?" before
                            // the user scrolls.
                            text = "${collection.parts.size} films",
                            style = SinemaTheme.typography.caption,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.xs),
                        )
                        collection.overview.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = SinemaTheme.typography.body,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = spacing.m),
                            )
                        }
                    }
                    state.error?.let { error ->
                        SinemaSnackbar(
                            message = error.asString(),
                            actionLabel = "RETRY",
                            onAction = { viewModel.retry() },
                            modifier = Modifier.padding(top = spacing.m),
                        )
                    }
                    Box(modifier = Modifier.padding(bottom = spacing.m))
                }
            }

            items(state.collection?.parts.orEmpty(), key = { it.id }) { movie ->
                SinemaMovieCard(
                    title = movie.title,
                    meta = movie.metaLine(),
                    rating = movie.voteAverage,
                    onClick = { onMovieClick(movie.id) },
                    width = Dp.Unspecified,
                    modifier = Modifier.fillMaxWidth(),
                    poster = {
                        movie.posterUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } ?: SinemaPosterPlaceholder()
                    },
                )
            }
        }

        if (state.isLoading && state.collection == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SinemaLoadingIndicator(size = 48.dp)
            }
        }
        CollectionBackButton(onBack)
    }
}

@Composable
private fun BoxScope.CollectionBackButton(onBack: () -> Unit) {
    val colors = SinemaTheme.colors
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(SinemaTheme.spacing.m)
            .size(40.dp)
            .clip(SinemaTheme.shapes.pill)
            .background(colors.surface.copy(alpha = 0.85f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = SinemaIcons.Back,
            contentDescription = "Back",
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
