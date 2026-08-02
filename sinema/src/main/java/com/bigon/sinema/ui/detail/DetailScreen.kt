package com.bigon.sinema.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.core.designsystem.components.SinemaCastCard
import com.bigon.core.designsystem.components.SinemaChip
import com.bigon.core.designsystem.components.SinemaFavoriteToggle
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.designsystem.components.SinemaPosterPlaceholder
import com.bigon.core.designsystem.components.SinemaPrimaryButton
import com.bigon.core.designsystem.components.SinemaSectionHeader
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.posterModifier

@Composable
fun DetailRoute(
    movieId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
    viewModel: DetailViewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
        key = "detail-$movieId",
    ) { factory -> factory.create(movieId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // System back is handled by the NavHost back stack; only the on-screen
    // affordance needs wiring.
    DetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        transition = transition,
        modifier = modifier,
    )
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    transition: PosterTransition? = null,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            Backdrop(state = state, transition = transition, onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = spacing.l)) {
                Text(
                    text = state.title.orEmpty(),
                    style = SinemaTheme.typography.display,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.l),
                )
                state.detail?.tagline?.let { tagline ->
                    Text(
                        text = tagline,
                        style = SinemaTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                }

                MetaRow(state = state, modifier = Modifier.padding(top = spacing.m))

                if (state.genres.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.s),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = spacing.m),
                    ) {
                        state.genres.forEach { genre ->
                            SinemaChip(label = genre, selected = false, onClick = {})
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                    modifier = Modifier.padding(top = spacing.xl),
                ) {
                    SinemaPrimaryButton(
                        text = "Watch trailer",
                        leadingIcon = SinemaIcons.Play,
                        enabled = state.detail?.trailerKey != null,
                        onClick = { /* playback arrives with the trailer feature */ },
                    )
                    SinemaFavoriteToggle(
                        checked = state.isFavorite,
                        onCheckedChange = { onIntent(DetailIntent.FavoriteToggled(it)) },
                    )
                }

                state.error?.let { error ->
                    SinemaSnackbar(
                        message = error.asString(),
                        actionLabel = "RETRY",
                        onAction = { onIntent(DetailIntent.Retry) },
                        modifier = Modifier.padding(top = spacing.l),
                    )
                }

                state.overview?.let { overview ->
                    SinemaSectionHeader(title = "Overview", modifier = Modifier.padding(top = spacing.xl))
                    Text(
                        text = overview,
                        style = SinemaTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.s),
                    )
                }

                state.detail?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
                    SinemaSectionHeader(title = "Cast", modifier = Modifier.padding(top = spacing.xl))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.m),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = spacing.m),
                    ) {
                        cast.forEach { member ->
                            SinemaCastCard(
                                name = member.name,
                                role = member.character,
                                avatar = {
                                    member.profileUrl?.let { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } ?: Icon(
                                        imageVector = SinemaIcons.Person,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(28.dp).align(Alignment.Center),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (state.showLoader) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SinemaLoadingIndicator(size = 48.dp)
            }
        }
    }
}

/**
 * Backdrop behind a scrim, with the poster overlapping it. The poster carries
 * the shared-element key, so it flies from the grid card into this position.
 */
@Composable
private fun Backdrop(
    state: DetailUiState,
    transition: PosterTransition?,
    onBack: () -> Unit,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    // Tall enough that the poster clears the back button beneath the status bar.
    Box(modifier = Modifier.fillMaxWidth().height(330.dp)) {
        state.backdropUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Scrim keeps title text legible over any artwork.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, colors.background.copy(alpha = 0.65f), colors.background),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(spacing.m)
                .size(40.dp)
                .clip(SinemaTheme.shapes.pill)
                .background(colors.surface.copy(alpha = 0.7f))
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = spacing.l, bottom = spacing.xs)
                .width(110.dp)
                .aspectRatio(2f / 3f)
                .clip(SinemaTheme.shapes.card)
                .then(transition.posterModifier(state.movieId)),
        ) {
            state.posterUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: SinemaPosterPlaceholder()
        }
    }
}

@Composable
private fun MetaRow(state: DetailUiState, modifier: Modifier = Modifier) {
    val colors = SinemaTheme.colors
    val parts = buildList {
        state.year?.let { add(it.toString()) }
        state.detail?.runtimeMinutes?.let { add("${it / 60}h ${it % 60}m") }
        state.rating?.let { add("★ ${(kotlin.math.round(it * 10) / 10)}") }
        state.detail?.voteCount?.takeIf { it > 0 }?.let { add("$it votes") }
    }
    if (parts.isEmpty()) return

    Text(
        text = parts.joinToString("  ·  "),
        style = SinemaTheme.typography.caption,
        color = colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
