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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bigon.tmdb.ui.BigonCastCard
import com.bigon.core.designsystem.components.BigonChip
import com.bigon.core.designsystem.components.BigonFavoriteToggle
import com.bigon.tmdb.ui.BigonMovieCard
import com.bigon.tmdb.ui.BigonPosterPlaceholder
import com.bigon.core.designsystem.components.BigonPrimaryButton
import com.bigon.core.designsystem.components.BigonSectionHeader
import com.bigon.core.designsystem.components.BigonSnackbar
import com.bigon.core.designsystem.components.BigonTonalButton
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.components.BigonShimmerText
import com.bigon.core.designsystem.components.BigonShimmerChip
import com.bigon.tmdb.ui.BigonShimmerCard
import com.bigon.tmdb.ui.BigonShimmerCastCard
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.tmdb.model.Review
import com.bigon.tmdb.model.WatchProviders
import com.bigon.core.ui.asString
import com.bigon.sinema.ui.PosterTransition
import com.bigon.sinema.ui.trailer.openTrailer
import com.bigon.sinema.ui.metaLine
import com.bigon.sinema.ui.posterModifier
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.bigon.core.designsystem.components.BigonShimmerBox

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
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors
    val activityContext = LocalContext.current

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
                state.title?.let { title ->
                    Text(
                        text = title,
                        style = BigonTheme.typography.display,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = spacing.l),
                    )
                } ?: BigonShimmerText(
                    style = BigonTheme.typography.display,
                    lastLineFraction = 0.7f,
                    modifier = Modifier.padding(top = spacing.l),
                )
                state.detail?.tagline?.let { tagline ->
                    Text(
                        text = tagline,
                        style = BigonTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                } ?: if (state.isDetailPending) {
                    // Reserved because 17 of 20 sampled trending titles carry a
                    // tagline: holding 49px for it means the three that do not
                    // close a small gap, while the seventeen that do stop
                    // shoving the whole screen down on arrival.
                    BigonShimmerText(
                        style = BigonTheme.typography.body,
                        lastLineFraction = 0.5f,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                } else Unit

                MetaRow(state = state, modifier = Modifier.padding(top = spacing.m))

                if (state.genres.isNotEmpty() || state.certification != null || state.isDetailPending) {
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
                            BigonChip(label = certification, selected = true, onClick = {})
                        } ?: if (state.isDetailPending) {
                            // Certification comes from release_dates, so it
                            // arrives with the response even when the genres
                            // beside it came from the cached row — which is why
                            // it used to appear a beat later and shove them
                            // sideways. Reserved: 14 of 20 sampled titles carry
                            // one in this region, 19 of 20 in the US.
                            BigonShimmerChip(labelFor = "PG-13", selected = true)
                        } else Unit

                        state.genres.forEach { genre ->
                            BigonChip(label = genre, selected = false, onClick = {})
                        }
                        if (state.genres.isEmpty() && state.isDetailPending) {
                            BigonShimmerChip(labelFor = "Adventure")
                            BigonShimmerChip(labelFor = "Action")
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                    modifier = Modifier.padding(top = spacing.xl),
                ) {
                    BigonPrimaryButton(
                        text = "Watch trailer",
                        leadingIcon = BigonIcons.Play,
                        enabled = state.detail?.trailerKey != null,
                        onClick = {
                            val key = state.detail?.trailerKey ?: return@BigonPrimaryButton
                            // A device with neither YouTube nor a browser is
                            // rare, but a button that appears to do nothing is
                            // worse than one that says why.
                            if (!activityContext.openTrailer(key)) {
                                onIntent(DetailIntent.TrailerUnavailable)
                            }
                        },
                    )
                    BigonFavoriteToggle(
                        checked = state.isFavorite,
                        onCheckedChange = { onIntent(DetailIntent.FavoriteToggled(it)) },
                    )
                }

                state.error?.let { error ->
                    BigonSnackbar(
                        message = error.asString(),
                        actionLabel = "RETRY",
                        onAction = { onIntent(DetailIntent.Retry) },
                        modifier = Modifier.padding(top = spacing.l),
                    )
                }

                // The heading is not data and needs no skeleton — only the
                // prose beneath it is waiting on the network.
                if (state.overview != null || state.isDetailPending) {
                    BigonSectionHeader(title = "Overview", modifier = Modifier.padding(top = spacing.xl))
                }
                state.overview?.let { overview ->
                    Text(
                        text = overview,
                        style = BigonTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.s),
                    )
                } ?: if (state.isDetailPending) {
                    BigonShimmerText(
                        style = BigonTheme.typography.body,
                        lines = 4,
                        lastLineFraction = 0.6f,
                        modifier = Modifier.padding(top = spacing.s),
                    )
                } else Unit

                state.detail?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
                    BigonSectionHeader(title = "Cast", modifier = Modifier.padding(top = spacing.xl))
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

                if (state.detail?.cast.isNullOrEmpty() && state.isDetailPending) {
                    BigonSectionHeader(title = "Cast", modifier = Modifier.padding(top = spacing.xl))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.m),
                        modifier = Modifier.padding(top = spacing.m),
                    ) {
                        // Five is roughly what fits across a phone, so the row
                        // fills the width it is about to occupy without
                        // pretending to know how long the billed cast is.
                        repeat(5) { BigonShimmerCastCard() }
                    }
                }

                state.detail?.collection?.let { collection ->
                    BigonSectionHeader(
                        title = "Part of a collection",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    BigonTonalButton(
                        text = collection.name,
                        onClick = { onCollectionClick(collection.id) },
                        modifier = Modifier.padding(top = spacing.m),
                    )
                }

                state.watchProviders?.let { providers ->
                    WhereToWatch(providers = providers)
                } ?: if (state.isDetailPending) {
                    WhereToWatchSkeleton()
                } else Unit

                if (state.recommendations.isEmpty() && state.isDetailPending) {
                    BigonSectionHeader(
                        title = "More like this",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.m),
                        modifier = Modifier.padding(top = spacing.m),
                    ) {
                        repeat(3) { BigonShimmerCard() }
                    }
                }

                if (state.recommendations.isNotEmpty()) {
                    BigonSectionHeader(
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
                            BigonMovieCard(
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
                                    } ?: BigonPosterPlaceholder()
                                },
                            )
                        }
                    }
                }

                if (state.keywords.isEmpty() && state.isDetailPending) {
                    BigonSectionHeader(
                        title = "Themes",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.s),
                        modifier = Modifier.padding(top = spacing.m),
                    ) {
                        repeat(4) { BigonShimmerChip() }
                    }
                }

                if (state.keywords.isNotEmpty()) {
                    BigonSectionHeader(
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
                            BigonChip(label = keyword, selected = false, onClick = {})
                        }
                    }
                }

                if (state.alternativeTitles.isNotEmpty()) {
                    BigonSectionHeader(
                        title = "Also known as",
                        modifier = Modifier.padding(top = spacing.xl),
                    )
                    Text(
                        text = state.alternativeTitles.joinToString("  ·  "),
                        style = BigonTheme.typography.body,
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
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

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
                .clip(BigonTheme.shapes.pill)
                // The scrim is what makes the icon legible over artwork; once
                // the bar itself is opaque it is redundant, so it fades out as
                // the bar fades in.
                .background(colors.surface.copy(alpha = 0.7f * (1f - progress)))
                .clickable(onClick = onBack),
        ) {
            Icon(
                imageVector = BigonIcons.Back,
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
                style = BigonTheme.typography.title,
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
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

    BigonSectionHeader(
        title = if (state.totalReviews > 0) "Reviews (${state.totalReviews})" else "Reviews",
        modifier = Modifier.padding(top = spacing.xl),
    )

    state.reviewsError?.let { error ->
        BigonSnackbar(
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
            // A spinner in a card-sized hole told the reader something was
            // happening but not what shape it would be; this is the card.
            ReviewCardSkeleton()
        }

        // "Load more" rides at the end of the row rather than sitting under it,
        // so it is where the reader's thumb already is when they run out.
        if (state.hasMoreReviews && !state.isLoadingReviews) {
            Box(
                modifier = Modifier
                    .width(REVIEW_CARD_WIDTH)
                    .height(REVIEW_CARD_HEIGHT)
                    .clip(BigonTheme.shapes.container)
                    .background(colors.surfaceVariant)
                    .clickable { onIntent(DetailIntent.MoreReviewsRequested) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Load more",
                    style = BigonTheme.typography.body,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

/**
 * The loading form of [ReviewCard], sharing its width, height, padding and
 * avatar so a review arriving does not resize the row it lands in.
 */
@Composable
private fun ReviewCardSkeleton() {
    val spacing = BigonTheme.spacing

    Column(
        modifier = Modifier
            .width(REVIEW_CARD_WIDTH)
            .height(REVIEW_CARD_HEIGHT)
            .clip(BigonTheme.shapes.container)
            .background(BigonTheme.colors.surface)
            .padding(spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigonShimmerBox(
                shape = BigonTheme.shapes.pill,
                modifier = Modifier.size(28.dp),
            )
            BigonShimmerText(
                style = BigonTheme.typography.body,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = spacing.s),
            )
        }
        BigonShimmerText(
            style = BigonTheme.typography.body,
            lines = 5,
            lastLineFraction = 0.55f,
            modifier = Modifier.padding(top = spacing.s),
        )
    }
}

/** Shared by the real provider tile and its skeleton. */
private val PROVIDER_LOGO_SIZE = 44.dp

private val REVIEW_CARD_WIDTH = 300.dp
private val REVIEW_CARD_HEIGHT = 200.dp

@Composable
private fun ReviewCard(review: Review) {
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

    Column(
        modifier = Modifier
            .width(REVIEW_CARD_WIDTH)
            .height(REVIEW_CARD_HEIGHT)
            .clip(BigonTheme.shapes.container)
            .background(colors.surface)
            .padding(spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(BigonTheme.shapes.pill)
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
                    imageVector = BigonIcons.Person,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = review.author,
                style = BigonTheme.typography.body,
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
                    style = BigonTheme.typography.caption,
                    color = colors.primary,
                )
            }
        }

        Text(
            text = review.content,
            style = BigonTheme.typography.body,
            color = colors.textSecondary,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.s),
        )
    }
}

/**
 * The loading form of [WhereToWatch]. Reserved because 13 of 20 sampled
 * trending titles have availability in a given region — a majority, so holding
 * the space costs the minority a small collapse and saves everyone else a shove.
 *
 * The heading drops the region rather than guessing it. That is a text change
 * on arrival, not a layout one: the header is one line either way.
 */
@Composable
private fun WhereToWatchSkeleton() {
    val spacing = BigonTheme.spacing

    BigonSectionHeader(
        title = "Where to watch",
        modifier = Modifier.padding(top = spacing.xl),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        modifier = Modifier.padding(top = spacing.m),
    ) {
        repeat(4) {
            BigonShimmerBox(
                shape = BigonTheme.shapes.badge,
                modifier = Modifier.size(PROVIDER_LOGO_SIZE),
            )
        }
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
private fun WhereToWatch(providers: WatchProviders) {
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

    // Subscription first — it is the answer to "can I just watch this now?".
    // Rent and buy follow, deduplicated against it so a service offering all
    // three appears once.
    val streaming = providers.streaming
    val alsoAvailable = (providers.rent + providers.buy)
        .distinctBy { it.id }
        .filter { rentOrBuy -> streaming.none { it.id == rentOrBuy.id } }

    // The region is part of the claim, not decoration: "where to watch" is
    // only true somewhere, and TMDB's answer differs sharply by country.
    BigonSectionHeader(
        title = "Where to watch in ${providers.region}",
        modifier = Modifier.padding(top = spacing.xl),
    )

    // The row states where the film is available and does nothing else. It
    // used to open TMDB's own web page, which is a worse answer than the logos
    // already on screen: the reader learns nothing new and loses the app.
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = spacing.m),
    ) {
        (streaming + alsoAvailable).forEach { provider ->
            // Softened with the badge radius rather than the container one.
            // These logos are full-bleed squares with the mark running to the
            // edge, so 16dp took a visible bite out of Prime's arrow and Apple
            // TV's wordmark; 8dp on a 44dp tile reads as rounded without
            // reaching the artwork.
            Box(
                modifier = Modifier
                    .size(PROVIDER_LOGO_SIZE)
                    .clip(BigonTheme.shapes.badge)
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
            style = BigonTheme.typography.caption,
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
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

    // Tall enough that the poster clears the back button beneath the status bar.
    Box(modifier = Modifier.fillMaxWidth().height(330.dp)) {
        state.paintedBackdropUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } ?: if (state.isDetailPending) {
            BigonShimmerBox(
                // Square-cornered, because the backdrop it replaces runs to the
                // edges of the screen.
                shape = RectangleShape,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Loaded, and this title genuinely has no backdrop — the scrim over
            // the background is the whole header, as it was before there was a
            // skeleton here. Unguarded, this shimmered for the life of the
            // screen: measured on Rabbit Test (TMDB 102841), a fully loaded
            // page down to its cast and recommendations, still sweeping.
            Unit
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
                .clip(BigonTheme.shapes.card)
                .then(transition.posterModifier(state.movieId)),
        ) {
            state.paintedPosterUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: if (state.isDetailPending) {
                BigonShimmerBox(modifier = Modifier.fillMaxSize())
            } else {
                // Loaded, and this title genuinely has no poster.
                BigonPosterPlaceholder()
            }
        }
    }
}

@Composable
private fun MetaRow(state: DetailUiState, modifier: Modifier = Modifier) {
    val colors = BigonTheme.colors

    val parts = buildList {
        state.year?.let { add(it.toString()) }
        state.detail?.runtimeMinutes?.let { add("${it / 60}h ${it % 60}m") }
        state.rating?.let { add("★ ${(kotlin.math.round(it * 10) / 10)}") }
        state.detail?.voteCount?.takeIf { it > 0 }?.let { add("$it votes") }
    }

    if (!state.isDetailPending) {
        if (parts.isEmpty()) return
        // Settled: one string, so it can ellipsize as a whole if the type scale
        // is large enough to overflow.
        Text(
            text = parts.joinToString(META_SEPARATOR),
            style = BigonTheme.typography.caption,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    // Still loading, and this line is the one place on the screen where the two
    // sources interleave: year and rating ride in on the cached row, runtime
    // and the vote count only arrive with the response. Rendering it as a
    // single joined string would mean either showing a half-line that grows
    // sideways as the rest lands, or withholding what is already known.
    // Measured on 20 trending titles: 19 carry a runtime, 20 a vote count.
    val slots: List<@Composable () -> Unit> = buildList {
        val year = state.year
        add { if (year != null) MetaText(year.toString()) else MetaPlaceholder("2026") }

        val runtime = state.detail?.runtimeMinutes
        add {
            if (runtime != null) MetaText("${runtime / 60}h ${runtime % 60}m")
            else MetaPlaceholder("1h 50m")
        }

        val rating = state.rating
        add {
            if (rating != null) MetaText("★ ${(kotlin.math.round(rating * 10) / 10)}")
            else MetaPlaceholder("★ 7.8")
        }

        // Vote count is detail-only, so while pending it is always a placeholder.
        add { MetaPlaceholder("1283 votes") }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        slots.forEachIndexed { index, slot ->
            if (index > 0) MetaText(META_SEPARATOR)
            slot()
        }
    }
}

/** The separator between meta values, shared by both forms of the row. */
private const val META_SEPARATOR = "  ·  "

@Composable
private fun MetaText(text: String) {
    Text(
        text = text,
        style = BigonTheme.typography.caption,
        color = BigonTheme.colors.textSecondary,
        maxLines = 1,
    )
}

/** Sized from a representative value, so it holds the width the real one needs. */
@Composable
private fun MetaPlaceholder(sample: String) {
    BigonShimmerText(
        style = BigonTheme.typography.caption,
        placeholderFor = sample,
    )
}
