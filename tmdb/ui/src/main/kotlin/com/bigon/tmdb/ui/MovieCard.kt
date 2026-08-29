package com.bigon.tmdb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.isSpecified
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.preview.MoviePreviewParameterProvider
import com.bigon.core.designsystem.preview.PreviewMovie
import com.bigon.core.designsystem.theme.BigonTheme
import kotlin.math.round

object BigonMovieCardDefaults {
    val Width: Dp = 120.dp
    const val PosterAspectRatio: Float = 2f / 3f

    /**
     * How many [BigonShimmerCard]s to lay out while a grid of these loads.
     * Three full rows on a phone, so the skeleton reaches the bottom of the
     * screen instead of stopping two thirds of the way down and reading as a
     * short list that has finished loading.
     *
     * Here rather than in each screen: Home and Search both show this grid, and
     * two private constants that must agree is exactly the drift this object
     * exists to prevent.
     */
    const val SkeletonCount: Int = 9
}

/**
 * BigonMovieCard — Home rows · Search grid · Favorites · Recommendations
 * (component gallery §Content). The poster is a slot so features can inject an
 * async image (Coil on Android, or any CMP image loader) without this module
 * depending on one. Pass [Dp.Unspecified] as [width] to let the parent (e.g. a
 * grid cell) size the card instead.
 */
@Composable
fun BigonMovieCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    rating: Double? = null,
    width: Dp = BigonMovieCardDefaults.Width,
    poster: @Composable BoxScope.() -> Unit = { BigonPosterPlaceholder() },
) {
    Column(
        // Deliberately unclipped. The card is a poster with two lines of text
        // under it, and the poster clips itself below — rounding the whole
        // column put a 12dp radius through the bottom corners of the *text*,
        // which sits flush to the left edge, and shaved the first character of
        // the meta line: "2026 · Romance" rendered with the 2 sliced in half.
        //
        // The cost is a rectangular ripple instead of a rounded one, which is
        // ordinary for a grid cell. The shape a reader sees is the poster's,
        // and that is still rounded.
        modifier = modifier
            .then(if (width.isSpecified) Modifier.width(width) else Modifier)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BigonMovieCardDefaults.PosterAspectRatio)
                .clip(BigonTheme.shapes.card),
        ) {
            poster()
            if (rating != null) {
                BigonRatingBadge(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(BigonTheme.spacing.s),
                )
            }
        }
        Text(
            text = title,
            style = BigonTheme.typography.cardTitle,
            color = BigonTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = BigonTheme.spacing.s),
        )
        if (meta != null) {
            Text(
                text = meta,
                style = BigonTheme.typography.caption,
                color = BigonTheme.colors.textSecondary,
                modifier = Modifier.padding(top = BigonTheme.spacing.xs / 2),
            )
        }
    }
}

/** Decorative gradient stand-in shown until a real poster image loads. */
@Composable
fun BigonPosterPlaceholder(modifier: Modifier = Modifier) {
    val stops = if (BigonTheme.colors.isDark) {
        listOf(Color(0xFF31404E), Color(0xFF22303C), Color(0xFF1B2733))
    } else {
        listOf(Color(0xFFC9D4DD), Color(0xFFAEBBC6), Color(0xFF93A3B1))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(stops)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = BigonIcons.Movie,
            contentDescription = null,
            tint = BigonTheme.colors.textPrimary,
            modifier = Modifier
                .size(30.dp)
                .alpha(0.35f),
        )
    }
}

/** Rating overlay on posters — always amber on a dark scrim, in both themes. */
@Composable
fun BigonRatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(BigonTheme.shapes.badge)
            .background(BadgeScrim)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = BigonIcons.Star,
            contentDescription = null,
            tint = BadgeAmber,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = " " + formatRating(rating),
            style = BigonTheme.typography.caption,
            color = BadgeAmber,
        )
    }
}

private val BadgeScrim = Color(0xA6000000) // black 65%
private val BadgeAmber = Color(0xFFFFB648)

private fun formatRating(rating: Double): String {
    val tenths = round(rating * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonMovieCardPreview() {
    BigonPreviewSurface {
        BigonMovieCard(
            title = "Midnight Reel",
            meta = "2026 · Thriller",
            rating = 8.4,
            onClick = {},
        )
    }
}

@BigonPreview
@Composable
private fun BigonMovieCardNoRatingPreview() {
    BigonPreviewSurface {
        BigonMovieCard(
            title = "Frame by Frame",
            meta = "2023 · Documentary",
            rating = null,
            onClick = {},
        )
    }
}

@BigonPreview
@Composable
private fun BigonMovieCardTitleOnlyPreview() {
    BigonPreviewSurface {
        BigonMovieCard(title = "Silver Screen", onClick = {})
    }
}

@BigonPreview
@Composable
private fun BigonMovieCardLongTitlePreview() {
    BigonPreviewSurface {
        BigonMovieCard(
            title = "A Very Long Movie Title That Wraps Onto Several Lines",
            meta = "2026 · Epic",
            rating = 9.7,
            onClick = {},
        )
    }
}

/** Sized by its parent instead of the 120dp default — the search-grid case. */
@BigonPreview
@Composable
private fun BigonMovieCardParentSizedPreview() {
    BigonPreviewSurface {
        BigonMovieCard(
            title = "Final Cut",
            meta = "2026 · Horror",
            rating = 8.9,
            onClick = {},
            width = Dp.Unspecified,
            modifier = Modifier.width(200.dp),
        )
    }
}

/** Every awkward title/rating combination, one render each. */
@BigonPreview
@Composable
private fun BigonMovieCardEdgeCasePreview(
    @PreviewParameter(MoviePreviewParameterProvider::class) movie: PreviewMovie,
) {
    BigonPreviewSurface {
        BigonMovieCard(
            title = movie.title,
            meta = movie.meta,
            rating = movie.rating,
            onClick = {},
        )
    }
}

@BigonFontScalePreview
@Composable
private fun BigonMovieCardFontScalePreview() {
    BigonPreviewSurface {
        BigonMovieCard(
            title = "The Long Take",
            meta = "2025 · Drama",
            rating = 7.9,
            onClick = {},
        )
    }
}

@BigonThemePreview
@Composable
private fun BigonPosterPlaceholderPreview() {
    BigonPreviewSurface {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(BigonMovieCardDefaults.PosterAspectRatio)
                .clip(BigonTheme.shapes.card),
        ) {
            BigonPosterPlaceholder()
        }
    }
}

@BigonThemePreview
@Composable
private fun BigonRatingBadgePreview() {
    BigonPreviewSurface {
        BigonRatingBadge(rating = 8.4)
    }
}

/** Rounding behaviour: whole numbers, halves, and a perfect score. */
@BigonPreview
@Composable
private fun BigonRatingBadgeRoundingPreview() {
    BigonPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BigonRatingBadge(rating = 10.0)
            BigonRatingBadge(rating = 8.0)
            BigonRatingBadge(rating = 7.25)
        }
    }
}
