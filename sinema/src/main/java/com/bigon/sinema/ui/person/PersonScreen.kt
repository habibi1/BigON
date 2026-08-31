package com.bigon.sinema.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.tmdb.ui.BigonMovieCard
import com.bigon.tmdb.ui.BigonPosterPlaceholder
import com.bigon.core.designsystem.components.BigonSectionHeader
import com.bigon.core.designsystem.components.BigonShimmerBox
import com.bigon.tmdb.ui.BigonShimmerCard
import com.bigon.core.designsystem.components.BigonSnackbar
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.tmdb.model.Credit
import com.bigon.tmdb.model.PersonDetail
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
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors
    val density = LocalDensity.current

    // The handover is measured against the bottom of the portrait block, not
    // the top of the name. The name begins its life exactly at the bar's lower
    // edge — the header is padded to clear the bar — so anchoring there gave it
    // no travel at all and the bar was fully shown before a finger touched the
    // screen. The portrait's bottom edge starts a portrait's height lower and
    // arrives at the bar only once the block really has been scrolled away.
    //
    // Measured rather than assumed, so a two-line name or a longer subtitle
    // moves the handover with it.
    val barBottomPx = WindowInsets.statusBars.getTop(density) +
        with(density) { PersonTopBarHeight.toPx() }
    val fadeDistancePx = with(density) { HandoverDistance.toPx() }
    var portraitBottomPx by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    // Read as a lambda, not a value: the bar reads it while drawing, so
    // scrolling repaints it without recomposing this screen on every frame.
    val barProgress = {
        (((barBottomPx + fadeDistancePx) - portraitBottomPx) / fadeDistancePx).coerceIn(0f, 1f)
    }

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
                        state.person?.let {
                            PersonHeader(
                                person = it,
                                onPortraitBottom = { bottom -> portraitBottomPx = bottom },
                            )
                        }
                        state.error?.let { error ->
                            BigonSnackbar(
                                message = error.asString(),
                                actionLabel = "RETRY",
                                onAction = { onIntent(PersonIntent.Retry) },
                                modifier = Modifier.padding(top = spacing.m),
                            )
                        }
                        if (!state.person?.filmography.isNullOrEmpty()) {
                            BigonSectionHeader(
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

        PersonTopBar(person = state.person, progress = barProgress, onBack = onBack)
    }
}

@Composable
private fun PersonHeader(person: PersonDetail, onPortraitBottom: (Float) -> Unit) {
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors
    // Biographies run to several hundred words; showing all of it pushes the
    // filmography off-screen, so it collapses until asked for.
    var expanded by remember { mutableStateOf(false) }

    // Clears the bar above it. Derived from the bar's own height rather than
    // written out, so the two cannot drift apart: the grid already insets its
    // content by `spacing.l`, which is why that comes back off.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PersonTopBarHeight - spacing.l)
            // Root coordinates, the same space the bar's bottom edge is in.
            // Once this scrolls far enough to be disposed it stops reporting,
            // and the last value it left is already past the handover — so the
            // bar stays where it is rather than flickering back.
            .onGloballyPositioned { onPortraitBottom(it.boundsInRoot().bottom) },
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2f / 3f)
                .clip(BigonTheme.shapes.card)
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
                imageVector = BigonIcons.Person,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(36.dp).align(Alignment.Center),
            )
        }

        Column(modifier = Modifier.padding(start = spacing.m)) {
            Text(
                text = person.name,
                style = BigonTheme.typography.title,
                color = colors.textPrimary,
            )
            listOfNotNull(person.knownForDepartment, person.lifespan)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    Text(
                        text = it.joinToString(" · "),
                        style = BigonTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                }
            person.placeOfBirth?.let {
                Text(
                    text = it,
                    style = BigonTheme.typography.caption,
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
            style = BigonTheme.typography.body,
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
                style = BigonTheme.typography.caption,
                color = colors.primary,
                modifier = Modifier
                    .padding(top = spacing.xs)
                    .clip(BigonTheme.shapes.pill)
                    .clickable { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun CreditCard(credit: Credit, onClick: () -> Unit) {
    BigonMovieCard(
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
            } ?: BigonPosterPlaceholder()
        },
    )
}

/**
 * Tall enough to hold the 40dp back button with [BigonSpacing.m] above and
 * below it, which is exactly where that button sat when it floated alone. The
 * bar arriving therefore moves nothing.
 */
private val PersonTopBarHeight = 64.dp

/** How far the portrait's lower edge travels while the bar fades in. */
private val HandoverDistance = 72.dp

/**
 * The bar over the filmography: back, and — once the large name has scrolled
 * under it — a small portrait and the name it replaces.
 *
 * Scroll-linked rather than a state flip. The reader is dragging the large name
 * upward, and a bar that appeared in one frame partway through that drag would
 * read as a second thing arriving rather than as the same name changing size.
 * Every animated property here is a function of one number, [progress], so the
 * handover runs at exactly the speed of the finger and reverses just as
 * smoothly.
 *
 * [progress] is a lambda, not a Float, and every reader of it is a draw or
 * layer callback. That keeps a scroll off the recomposition path entirely: the
 * bar repaints per frame, it never recomposes.
 */
@Composable
private fun BoxScope.PersonTopBar(
    person: PersonDetail?,
    progress: () -> Float,
    onBack: () -> Unit,
) {
    val colors = BigonTheme.colors
    val spacing = BigonTheme.spacing

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            // Painted rather than `background(...)`: a background colour is
            // read at composition, and this one changes every frame.
            .drawBehind {
                val shown = progress()
                if (shown <= 0f) return@drawBehind
                drawRect(colors.surface.copy(alpha = shown))
                drawRect(
                    color = colors.outline.copy(alpha = shown),
                    topLeft = Offset(0f, size.height - 1f),
                    size = Size(size.width, 1f),
                )
            }
            .statusBarsPadding()
            .height(PersonTopBarHeight)
            .padding(horizontal = spacing.m),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(BigonTheme.shapes.pill)
                    // The scrim under the icon is what makes it legible against
                    // a photograph, and it is only needed until the bar itself
                    // is opaque — so it fades out as the bar fades in.
                    .drawBehind {
                        drawRect(colors.surface.copy(alpha = 0.85f * (1f - progress())))
                    }
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

            if (person != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = spacing.m)
                        .graphicsLayer {
                            val shown = progress()
                            alpha = shown
                            // Rises into place and settles at its own size, so
                            // the portrait reads as the large one arriving
                            // rather than as a new badge appearing.
                            translationY = (1f - shown) * 10.dp.toPx()
                            val scale = 0.8f + 0.2f * shown
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(BigonTheme.shapes.pill)
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
                            imageVector = BigonIcons.Person,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp).align(Alignment.Center),
                        )
                    }

                    Text(
                        text = person.name,
                        style = BigonTheme.typography.title,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(start = spacing.s),
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonSkeleton() {
    val spacing = BigonTheme.spacing
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(spacing.l)) {
        // Same clearance as the loaded header, so nothing shifts on arrival.
        Row(modifier = Modifier.padding(top = PersonTopBarHeight - spacing.l)) {
            BigonShimmerBox(modifier = Modifier.width(120.dp).aspectRatio(2f / 3f))
            Column(modifier = Modifier.padding(start = spacing.m)) {
                BigonShimmerBox(
                    shape = BigonTheme.shapes.pill,
                    modifier = Modifier.width(160.dp).height(22.dp),
                )
                BigonShimmerBox(
                    shape = BigonTheme.shapes.pill,
                    modifier = Modifier.padding(top = spacing.s).width(110.dp).height(14.dp),
                )
            }
        }
        repeat(3) {
            BigonShimmerBox(
                shape = BigonTheme.shapes.pill,
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
            repeat(3) { BigonShimmerCard(width = 120.dp) }
        }
    }
}
