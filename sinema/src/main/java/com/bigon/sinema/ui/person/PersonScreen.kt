package com.bigon.sinema.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.core.designsystem.components.SinemaMovieCard
import com.bigon.core.designsystem.components.SinemaPosterPlaceholder
import com.bigon.core.designsystem.components.SinemaSectionHeader
import com.bigon.core.designsystem.components.SinemaShimmerBox
import com.bigon.core.designsystem.components.SinemaShimmerCard
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.model.Credit
import com.bigon.core.model.PersonDetail
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.metaLine

@Composable
fun PersonRoute(
    personId: Long,
    onBack: () -> Unit,
    onMovieClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonViewModel = hiltViewModel<PersonViewModel, PersonViewModel.Factory>(
        key = "person-$personId",
    ) { factory -> factory.create(personId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PersonScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onMovieClick = { id ->
            viewModel.onIntent(PersonIntent.MovieClicked(id))
            onMovieClick(id)
        },
        modifier = modifier,
    )
}

@Composable
fun PersonScreen(
    state: PersonUiState,
    onIntent: (PersonIntent) -> Unit,
    onBack: () -> Unit,
    onMovieClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (state.showSkeleton) {
            PersonSkeleton()
        } else {
            // The filmography is the bulk of the screen and is already a grid,
            // so the biography rides as a full-span header item rather than
            // being a separate scrolling container above it — two nested
            // scrollers would fight each other.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.l),
                verticalArrangement = Arrangement.spacedBy(spacing.l),
                contentPadding = PaddingValues(spacing.l),
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        state.person?.let { PersonHeader(it) }
                        state.error?.let { error ->
                            SinemaSnackbar(
                                message = error.asString(),
                                actionLabel = "RETRY",
                                onAction = { onIntent(PersonIntent.Retry) },
                                modifier = Modifier.padding(top = spacing.m),
                            )
                        }
                        if (!state.person?.filmography.isNullOrEmpty()) {
                            SinemaSectionHeader(
                                title = "Filmography",
                                modifier = Modifier.padding(top = spacing.xl, bottom = spacing.s),
                            )
                        }
                    }
                }

                items(state.person?.filmography.orEmpty(), key = { it.movie.id }) { credit ->
                    CreditCard(credit = credit, onClick = { onMovieClick(credit.movie.id) })
                }
            }
        }

        BackButton(onBack = onBack)
    }
}

@Composable
private fun PersonHeader(person: PersonDetail) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors
    // Biographies run to several hundred words; showing all of it pushes the
    // filmography off-screen, so it collapses until asked for.
    var expanded by remember { mutableStateOf(false) }

    // Clears the floating back button, which ends 52dp below the status bar.
    Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.xxl + spacing.l)) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2f / 3f)
                .clip(SinemaTheme.shapes.card)
                .background(colors.surfaceVariant),
        ) {
            person.profileUrl?.let { url ->
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
                modifier = Modifier.size(36.dp).align(Alignment.Center),
            )
        }

        Column(modifier = Modifier.padding(start = spacing.m)) {
            Text(
                text = person.name,
                style = SinemaTheme.typography.title,
                color = colors.textPrimary,
            )
            listOfNotNull(person.knownForDepartment, person.lifespan)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Text(
                        text = it.joinToString(" · "),
                        style = SinemaTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                }
            person.placeOfBirth?.let {
                Text(
                    text = it,
                    style = SinemaTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
        }
    }

    person.biography.takeIf { it.isNotBlank() }?.let { bio ->
        // Many biographies are a single sentence. Offering "Read more" on text
        // that is already whole makes the affordance a lie, so the layout pass
        // decides: only a bio that actually clipped gets the toggle.
        var clipped by remember(bio) { mutableStateOf(false) }
        Text(
            text = bio,
            style = SinemaTheme.typography.body,
            color = colors.textSecondary,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // Only trust the collapsed pass: once expanded, nothing overflows
                // and the toggle would disappear out from under the user.
                if (!expanded) clipped = result.hasVisualOverflow
            },
            modifier = Modifier.padding(top = spacing.l),
        )
        if (clipped) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                style = SinemaTheme.typography.caption,
                color = colors.primary,
                modifier = Modifier
                    .padding(top = spacing.xs)
                    .clip(SinemaTheme.shapes.pill)
                    .clickable { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun CreditCard(credit: Credit, onClick: () -> Unit) {
    SinemaMovieCard(
        title = credit.movie.title,
        // The role is what distinguishes two credits on the same screen, so it
        // wins the meta line over the genre the card usually shows.
        meta = credit.role ?: credit.movie.metaLine(),
        rating = credit.movie.voteAverage,
        onClick = onClick,
        width = Dp.Unspecified,
        modifier = Modifier.fillMaxWidth(),
        poster = {
            credit.movie.posterUrl?.let { url ->
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

@Composable
private fun BoxScope.BackButton(onBack: () -> Unit) {
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

@Composable
private fun PersonSkeleton() {
    val spacing = SinemaTheme.spacing
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(spacing.l)) {
        // Same clearance as the loaded header, so nothing shifts on arrival.
        Row(modifier = Modifier.padding(top = spacing.xxl + spacing.l)) {
            SinemaShimmerBox(modifier = Modifier.width(120.dp).aspectRatio(2f / 3f))
            Column(modifier = Modifier.padding(start = spacing.m)) {
                SinemaShimmerBox(
                    shape = SinemaTheme.shapes.pill,
                    modifier = Modifier.width(160.dp).height(22.dp),
                )
                SinemaShimmerBox(
                    shape = SinemaTheme.shapes.pill,
                    modifier = Modifier.padding(top = spacing.s).width(110.dp).height(14.dp),
                )
            }
        }
        repeat(3) {
            SinemaShimmerBox(
                shape = SinemaTheme.shapes.pill,
                modifier = Modifier
                    .padding(top = if (it == 0) spacing.l else spacing.s)
                    .fillMaxWidth(if (it == 2) 0.6f else 1f)
                    .height(14.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            modifier = Modifier.padding(top = spacing.xl),
        ) {
            repeat(3) { SinemaShimmerCard(width = 120.dp) }
        }
    }
}
