package com.bigon.sinema.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.core.designsystem.components.SinemaCastCard
import com.bigon.core.designsystem.components.SinemaChip
import com.bigon.core.designsystem.components.SinemaFavoriteToggle
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.designsystem.components.SinemaMovieCard
import com.bigon.core.designsystem.components.SinemaPosterPlaceholder
import com.bigon.core.designsystem.components.SinemaPrimaryButton
import com.bigon.core.designsystem.components.SinemaSectionHeader
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.components.SinemaTonalButton
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.core.model.Review
import com.bigon.core.model.WatchProviders
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.metaLine
import com.bigon.sinema.ui.posterModifier
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.bigon.core.designsystem.components.SinemaShimmerBox

@Composable
fun DetailRoute(
    movieId: Long,
    onBack: () -> Unit,
    onMovieClick: (Long) -> Unit,
    onPersonClick: (Long) -> Unit = {},
    onCollectionClick: (Long) -> Unit = {},
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
        onRecommendationClick = { id ->
            viewModel.onIntent(DetailIntent.RecommendationClicked(id))
            onMovieClick(id)
        },
        onPersonClick = onPersonClick,
        onCollectionClick = onCollectionClick,
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
    onRecommendationClick: (Long) -> Unit = {},
    onPersonClick: (Long) -> Unit = {},
    onCollectionClick: (Long) -> Unit = {},
    transition: PosterTransition? = null,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors
    val uriHandler = LocalUriHandler.current

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // The logo takes over from the title only once the title itself has
    // scrolled away, so the two are never on screen at the same time saying
    // the same thing. Derived rather than remembered: it is a pure function of
    // scroll, and recomputing beats keeping a second copy in sync.
    val toolbarProgress by remember(density) {
        derivedStateOf {
            val start = with(density) { TOOLBAR_FADE_START.toPx() }
            val distance = with(density) { TOOLBAR_FADE_DISTANCE.toPx() }
            ((scrollState.value - start) / distance).coerceIn(0f, 1f)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

            Backdrop(state = state, transition = transition)

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

                if (state.genres.isNotEmpty() || state.certification != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.s),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = spacing.m),
                    ) {
                        // Age rating leads the row: it is the one chip a parent
                        // scans for, and it is selected-styled so it reads as a
                        // rating rather than another genre.
                        state.certification?.let { certification ->
                            SinemaChip(label = certification, selected = true, onClick = {})
                        }
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
                    state.imdbUrl?.let { url ->
                        SinemaTonalButton(
                            text = "IMDb",
                            onClick = {
                                onIntent(DetailIntent.ImdbClicked)
                                uriHandler.openUri(url)
                            },
                        )
                    }
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
                            // Faces are a destination now; before this the cast
                            // row was the one place the app showed a name it
                            // could tell you nothing more about.
                            Box(modifier = Modifier.clickable { onPersonClick(member.id) }) {
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

                state.detail?.collection?.let { collection ->
                    SinemaSectionHeader(
                        title = "Part of a collection",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    SinemaTonalButton(
                        text = collection.name,
                        onClick = { onCollectionClick(collection.id) },
                        modifier = Modifier.padding(top = spacing.m),
                    )
                }

                state.watchProviders?.let { providers ->
                    WhereToWatch(
                        providers = providers,
                        onOpen = {
                            onIntent(DetailIntent.WatchProvidersClicked)
                            providers.link?.let(uriHandler::openUri)
                        },
                    )
                }

                if (state.recommendations.isNotEmpty()) {
                    SinemaSectionHeader(
                        title = "More like this",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.m),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = spacing.m),
                    ) {
                        state.recommendations.forEach { movie ->
                            SinemaMovieCard(
                                title = movie.title,
                                meta = movie.metaLine(),
                                rating = movie.voteAverage,
                                onClick = { onRecommendationClick(movie.id) },
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
                }

                if (state.keywords.isNotEmpty()) {
                    SinemaSectionHeader(
                        title = "Themes",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.s),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = spacing.m),
                    ) {
                        state.keywords.forEach { keyword ->
                            SinemaChip(label = keyword, selected = false, onClick = {})
                        }
                    }
                }

                if (state.alternativeTitles.isNotEmpty()) {
                    SinemaSectionHeader(
                        title = "Also known as",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    Text(
                        text = state.alternativeTitles.joinToString("  ·  "),
                        style = SinemaTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.s),
                    )
                }

                if (state.showReviews) {
                    Reviews(state = state, onIntent = onIntent)
                }

                // Breathing room under the last section; the screen scrolls
                // edge-to-edge with no navigation bar beneath it.
                Spacer(modifier = Modifier.height(spacing.xxl))
            }
        }

        // Skeleton first, toolbar second: the back button has to stay on top
        // and tappable while loading, or a slow request traps the user.
        if (state.showLoader) {
            DetailSkeleton(modifier = Modifier.fillMaxSize().background(colors.background))
        }

        DetailToolbar(
            logoUrl = state.logoUrl,
            title = state.title,
            progress = toolbarProgress,
            onBack = onBack,
        )
    }
}

/** Where the logo starts and finishes fading in, measured from the top. */
private val TOOLBAR_FADE_START = 220.dp
private val TOOLBAR_FADE_DISTANCE = 80.dp
private val TOOLBAR_HEIGHT = 56.dp

/** Breathing room above and below the title mark inside the bar. */
private val TOOLBAR_LOGO_INSET = 9.dp

/** Share of the bar's centre slot a logo may occupy at its widest. */
private const val TOOLBAR_LOGO_MAX_WIDTH = 0.62f

/**
 * Sticky toolbar over the scrolling content.
 *
 * It is always present rather than appearing on scroll: the back button has to
 * stay reachable at every offset, and a control that materialises underneath a
 * moving thumb is worse than one that was there all along. What changes with
 * [progress] is the *backing* — transparent over the backdrop, opaque once
 * content would otherwise run under it — and the logo, which fades in only
 * after the title text has left the screen.
 */
@Composable
private fun BoxScope.DetailToolbar(
    logoUrl: String?,
    title: String?,
    progress: Float,
    onBack: () -> Unit,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = progress))
            .statusBarsPadding()
            .height(TOOLBAR_HEIGHT)
            .padding(horizontal = spacing.m),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(SinemaTheme.shapes.pill)
                // The scrim is what makes the icon legible over artwork; once
                // the bar itself is opaque it is redundant, so it fades out as
                // the bar fades in.
                .background(colors.surface.copy(alpha = 0.7f * (1f - progress)))
                .clickable(onClick = onBack),
        ) {
            Icon(
                imageVector = SinemaIcons.Back,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            logoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxHeight()
                        // Two constraints, because title treatments vary wildly
                        // in shape. The vertical inset keeps the mark from
                        // sitting flush against the bar edges, which reads as
                        // cramped however good the artwork is; the width cap
                        // stops a very landscape logo — some are 6:1 or wider —
                        // from spanning the bar and crowding the back button.
                        // With ContentScale.Fit, whichever limit binds first
                        // wins, so tall marks are bounded by height and wide
                        // ones by width without either being distorted.
                        .fillMaxWidth(TOOLBAR_LOGO_MAX_WIDTH)
                        .padding(vertical = TOOLBAR_LOGO_INSET)
                        .graphicsLayer { alpha = progress },
                )
            } ?: Text(
                // No logo for this title: the text stands in, so the toolbar
                // still says what you are looking at once the header is gone.
                text = title.orEmpty(),
                style = SinemaTheme.typography.title,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = progress },
            )
        }

        // Balances the back button so the logo sits on the true centre line.
        Spacer(modifier = Modifier.size(40.dp))
    }
}

/**
 * Skeleton of the real layout rather than a centred spinner.
 *
 * Detail is a tall, structured screen, and a lone spinner in the middle of it
 * says nothing about what is coming. These blocks occupy the same positions as
 * the backdrop, poster, title, chips, actions and overview, so the arrival of
 * content is a fill rather than a jump.
 */
@Composable
private fun DetailSkeleton(modifier: Modifier = Modifier) {
    val spacing = SinemaTheme.spacing

    Column(modifier = modifier) {
        SinemaShimmerBox(
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().height(330.dp),
        )

        Column(modifier = Modifier.padding(horizontal = spacing.l)) {
            // Poster, overlapping the backdrop exactly as the real one does.
            SinemaShimmerBox(
                modifier = Modifier
                    .padding(top = spacing.l)
                    .width(110.dp)
                    .height(165.dp),
            )
            SinemaShimmerBox(
                shape = SinemaTheme.shapes.pill,
                modifier = Modifier.padding(top = spacing.l).fillMaxWidth(0.7f).height(28.dp),
            )
            SinemaShimmerBox(
                shape = SinemaTheme.shapes.pill,
                modifier = Modifier.padding(top = spacing.s).fillMaxWidth(0.45f).height(16.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                modifier = Modifier.padding(top = spacing.m),
            ) {
                repeat(3) {
                    SinemaShimmerBox(
                        shape = SinemaTheme.shapes.pill,
                        modifier = Modifier.width(84.dp).height(32.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                modifier = Modifier.padding(top = spacing.xl),
            ) {
                SinemaShimmerBox(
                    shape = SinemaTheme.shapes.pill,
                    modifier = Modifier.width(180.dp).height(48.dp),
                )
                SinemaShimmerBox(
                    shape = SinemaTheme.shapes.pill,
                    modifier = Modifier.size(48.dp),
                )
            }

            repeat(4) { line ->
                SinemaShimmerBox(
                    shape = SinemaTheme.shapes.pill,
                    modifier = Modifier
                        .padding(top = if (line == 0) spacing.xl else spacing.s)
                        // The last line stops short, the way a paragraph does.
                        .fillMaxWidth(if (line == 3) 0.6f else 1f)
                        .height(14.dp),
                )
            }
        }
    }
}

/**
 * Reviews, as a horizontally scrolling row of cards.
 *
 * A stacked list put an unbounded amount of prose in the middle of the screen
 * and pushed everything below it out of reach. A row keeps the section a fixed
 * height whatever the reviews say, and matches how cast and recommendations
 * already read — every "there is more sideways" affordance on this screen
 * behaves the same way.
 *
 * Cards are fixed-width and clipped rather than sized to content: reviews vary
 * from one line to several thousand words, and letting that drive layout would
 * make the row's height a lottery.
 */
@Composable
private fun Reviews(
    state: DetailUiState,
    onIntent: (DetailIntent) -> Unit,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    SinemaSectionHeader(
        title = if (state.totalReviews > 0) "Reviews (${state.totalReviews})" else "Reviews",
        modifier = Modifier.padding(top = spacing.xl),
    )

    state.reviewsError?.let { error ->
        SinemaSnackbar(
            message = error.asString(),
            actionLabel = "RETRY",
            onAction = { onIntent(DetailIntent.ReviewsRequested) },
            modifier = Modifier.padding(top = spacing.m),
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = spacing.m),
    ) {
        state.reviews.forEach { review ->
            ReviewCard(review = review)
        }

        if (state.isLoadingReviews) {
            Box(
                modifier = Modifier.width(REVIEW_CARD_WIDTH).height(REVIEW_CARD_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                SinemaLoadingIndicator(size = 28.dp)
            }
        }

        // "Load more" rides at the end of the row rather than sitting under it,
        // so it is where the reader's thumb already is when they run out.
        if (state.hasMoreReviews && !state.isLoadingReviews) {
            Box(
                modifier = Modifier
                    .width(REVIEW_CARD_WIDTH)
                    .height(REVIEW_CARD_HEIGHT)
                    .clip(SinemaTheme.shapes.container)
                    .background(colors.surfaceVariant)
                    .clickable { onIntent(DetailIntent.MoreReviewsRequested) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Load more",
                    style = SinemaTheme.typography.body,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

private val REVIEW_CARD_WIDTH = 300.dp
private val REVIEW_CARD_HEIGHT = 200.dp

@Composable
private fun ReviewCard(review: Review) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Column(
        modifier = Modifier
            .width(REVIEW_CARD_WIDTH)
            .height(REVIEW_CARD_HEIGHT)
            .clip(SinemaTheme.shapes.container)
            .background(colors.surface)
            .padding(spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(SinemaTheme.shapes.pill)
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                review.avatarUrl?.let { url ->
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
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = review.author,
                style = SinemaTheme.typography.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = spacing.s),
            )
            review.rating?.let { rating ->
                Text(
                    text = "★ %.1f".format(rating),
                    style = SinemaTheme.typography.caption,
                    color = colors.primary,
                )
            }
        }

        Text(
            text = review.content,
            style = SinemaTheme.typography.body,
            color = colors.textSecondary,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.s),
        )
    }
}

/**
 * Streaming availability for one region.
 *
 * TMDB sources this from JustWatch, whose terms require that the data links
 * back rather than being presented as ours — so the whole section is a single
 * affordance that opens TMDB's own where-to-watch page, and the logos are
 * indicative rather than deep links into each service.
 */
@Composable
private fun WhereToWatch(
    providers: WatchProviders,
    onOpen: () -> Unit,
) {
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    // Subscription first — it is the answer to "can I just watch this now?".
    // Rent and buy follow, deduplicated against it so a service offering all
    // three appears once.
    val streaming = providers.streaming
    val alsoAvailable = (providers.rent + providers.buy)
        .distinctBy { it.id }
        .filter { rentOrBuy -> streaming.none { it.id == rentOrBuy.id } }

    // The region is part of the claim, not decoration: "where to watch" is
    // only true somewhere, and TMDB's answer differs sharply by country.
    SinemaSectionHeader(
        title = "Where to watch in ${providers.region}",
        modifier = Modifier.padding(top = spacing.xl),
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = spacing.m)
            .clip(SinemaTheme.shapes.container)
            .clickable(onClick = onOpen),
    ) {
        (streaming + alsoAvailable).forEach { provider ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SinemaTheme.shapes.container)
                    .background(colors.surfaceVariant),
            ) {
                provider.logoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = provider.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (streaming.isEmpty() && alsoAvailable.isNotEmpty()) {
        Text(
            text = "Available to rent or buy",
            style = SinemaTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.xs),
        )
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
