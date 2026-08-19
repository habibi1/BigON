package com.bigon.sinema.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.tmdb.ui.BigonCastCard
import com.bigon.core.designsystem.components.BigonChip
import com.bigon.core.designsystem.components.BigonLoadingIndicator
import com.bigon.core.designsystem.components.BigonSectionHeader
import com.bigon.core.designsystem.components.BigonSnackbar
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.core.ui.asString

/**
 * Series detail — a sibling of the movie screen rather than a reuse of it.
 *
 * The shared half (backdrop, cast, providers) looks the same; the divergent
 * half is what justifies the separate screen: seasons and episode counts and a
 * running status in place of a runtime, and no recommendations row, because
 * TMDB's TV recommendations are markedly thinner than its film ones.
 */
@Composable
fun TvDetailRoute(
    tvId: Long,
    onBack: () -> Unit,
    onPersonClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvDetailViewModel = hiltViewModel<TvDetailViewModel, TvDetailViewModel.Factory>(
        key = "tv-$tvId",
    ) { factory -> factory.create(tvId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                state.show?.backdropUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, colors.background.copy(alpha = 0.65f), colors.background),
                        ),
                    ),
                )
            }

            Column(modifier = Modifier.padding(horizontal = spacing.l)) {
                state.show?.let { show ->
                    Text(
                        text = show.name,
                        style = BigonTheme.typography.display,
                        color = colors.textPrimary,
                    )
                    show.tagline?.let {
                        Text(
                            text = it,
                            style = BigonTheme.typography.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.xs),
                        )
                    }

                    // Seasons and episodes are the series-specific facts; a
                    // runtime would be meaningless here.
                    Text(
                        text = listOfNotNull(
                            show.airedRange,
                            "${show.numberOfSeasons} season${if (show.numberOfSeasons == 1) "" else "s"}",
                            "${show.numberOfEpisodes} episodes".takeIf { show.numberOfEpisodes > 0 },
                            show.voteAverage?.let { "★ %.1f".format(it) },
                            show.status,
                        ).joinToString("  ·  "),
                        style = BigonTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.m),
                    )

                    if (show.genres.isNotEmpty() || show.certification != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.s),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = spacing.m),
                        ) {
                            show.certification?.let { BigonChip(label = it, selected = true, onClick = {}) }
                            show.genres.forEach { BigonChip(label = it, selected = false, onClick = {}) }
                        }
                    }

                    show.overview.takeIf { it.isNotBlank() }?.let {
                        BigonSectionHeader(title = "Overview", modifier = Modifier.padding(top = spacing.xl))
                        Text(
                            text = it,
                            style = BigonTheme.typography.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.s),
                        )
                    }

                    if (show.cast.isNotEmpty()) {
                        BigonSectionHeader(title = "Cast", modifier = Modifier.padding(top = spacing.xl))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.m),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = spacing.m),
                        ) {
                            show.cast.forEach { member ->
                                Box(modifier = Modifier.clickable { onPersonClick(member.id) }) {
                                    BigonCastCard(
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
                                                imageVector = BigonIcons.Person,
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

                state.error?.let { error ->
                    BigonSnackbar(
                        message = error.asString(),
                        actionLabel = "RETRY",
                        onAction = { viewModel.retry() },
                        modifier = Modifier.padding(top = spacing.l),
                    )
                }

                Box(modifier = Modifier.height(spacing.xxl))
            }
        }

        if (state.showSkeleton) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BigonLoadingIndicator(size = 48.dp)
            }
        }
        TvBackButton(onBack)
    }
}

@Composable
private fun BoxScope.TvBackButton(onBack: () -> Unit) {
    val colors = BigonTheme.colors
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(BigonTheme.spacing.m)
            .size(40.dp)
            .clip(BigonTheme.shapes.pill)
            .background(colors.surface.copy(alpha = 0.7f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = BigonIcons.Back,
            contentDescription = "Back",
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
