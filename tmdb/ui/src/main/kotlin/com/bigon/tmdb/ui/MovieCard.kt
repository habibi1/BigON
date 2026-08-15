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
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.preview.MoviePreviewParameterProvider
import com.bigon.core.designsystem.preview.PreviewMovie
import com.bigon.core.designsystem.theme.SinemaTheme
import kotlin.math.round

object SinemaMovieCardDefaults {
    val Width: Dp = 120.dp
    const val PosterAspectRatio: Float = 2f / 3f
}

/**
 * SinemaMovieCard — Home rows · Search grid · Favorites · Recommendations
 * (component gallery §Content). The poster is a slot so features can inject an
 * async image (Coil on Android, or any CMP image loader) without this module
 * depending on one. Pass [Dp.Unspecified] as [width] to let the parent (e.g. a
 * grid cell) size the card instead.
 */
@Composable
fun SinemaMovieCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    rating: Double? = null,
    width: Dp = SinemaMovieCardDefaults.Width,
    poster: @Composable BoxScope.() -> Unit = { SinemaPosterPlaceholder() },
) {
    Column(
        modifier = modifier
            .then(if (width.isSpecified) Modifier.width(width) else Modifier)
            .clip(SinemaTheme.shapes.card)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SinemaMovieCardDefaults.PosterAspectRatio)
                .clip(SinemaTheme.shapes.card),
        ) {
            poster()
            if (rating != null) {
                SinemaRatingBadge(
                    rating = rating,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(SinemaTheme.spacing.s),
                )
            }
        }
        Text(
            text = title,
            style = SinemaTheme.typography.cardTitle,
            color = SinemaTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = SinemaTheme.spacing.s),
        )
        if (meta != null) {
            Text(
                text = meta,
                style = SinemaTheme.typography.caption,
                color = SinemaTheme.colors.textSecondary,
                modifier = Modifier.padding(top = SinemaTheme.spacing.xs / 2),
            )
        }
    }
}

/** Decorative gradient stand-in shown until a real poster image loads. */
@Composable
fun SinemaPosterPlaceholder(modifier: Modifier = Modifier) {
    val stops = if (SinemaTheme.colors.isDark) {
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
            imageVector = SinemaIcons.Movie,
            contentDescription = null,
            tint = SinemaTheme.colors.textPrimary,
            modifier = Modifier
                .size(30.dp)
                .alpha(0.35f),
        )
    }
}

/** Rating overlay on posters — always amber on a dark scrim, in both themes. */
@Composable
fun SinemaRatingBadge(rating: Double, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(SinemaTheme.shapes.badge)
            .background(BadgeScrim)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Icon(
            imageVector = SinemaIcons.Star,
            contentDescription = null,
            tint = BadgeAmber,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = " " + formatRating(rating),
            style = SinemaTheme.typography.caption,
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

@SinemaThemePreview
@Composable
private fun SinemaMovieCardPreview() {
    SinemaPreviewSurface {
        SinemaMovieCard(
            title = "Midnight Reel",
            meta = "2026 · Thriller",
            rating = 8.4,
            onClick = {},
        )
    }
}

@SinemaPreview
@Composable
private fun SinemaMovieCardNoRatingPreview() {
    SinemaPreviewSurface {
        SinemaMovieCard(
            title = "Frame by Frame",
            meta = "2023 · Documentary",
            rating = null,
            onClick = {},
        )
    }
}

@SinemaPreview
@Composable
private fun SinemaMovieCardTitleOnlyPreview() {
    SinemaPreviewSurface {
        SinemaMovieCard(title = "Silver Screen", onClick = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaMovieCardLongTitlePreview() {
    SinemaPreviewSurface {
        SinemaMovieCard(
            title = "A Very Long Movie Title That Wraps Onto Several Lines",
            meta = "2026 · Epic",
            rating = 9.7,
            onClick = {},
        )
    }
}

/** Sized by its parent instead of the 120dp default — the search-grid case. */
@SinemaPreview
@Composable
private fun SinemaMovieCardParentSizedPreview() {
    SinemaPreviewSurface {
        SinemaMovieCard(
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
@SinemaPreview
@Composable
private fun SinemaMovieCardEdgeCasePreview(
    @PreviewParameter(MoviePreviewParameterProvider::class) movie: PreviewMovie,
) {
    SinemaPreviewSurface {
        SinemaMovieCard(
            title = movie.title,
            meta = movie.meta,
            rating = movie.rating,
            onClick = {},
        )
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaMovieCardFontScalePreview() {
    SinemaPreviewSurface {
        SinemaMovieCard(
            title = "The Long Take",
            meta = "2025 · Drama",
            rating = 7.9,
            onClick = {},
        )
    }
}

@SinemaThemePreview
@Composable
private fun SinemaPosterPlaceholderPreview() {
    SinemaPreviewSurface {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(SinemaMovieCardDefaults.PosterAspectRatio)
                .clip(SinemaTheme.shapes.card),
        ) {
            SinemaPosterPlaceholder()
        }
    }
}

@SinemaThemePreview
@Composable
private fun SinemaRatingBadgePreview() {
    SinemaPreviewSurface {
        SinemaRatingBadge(rating = 8.4)
    }
}

/** Rounding behaviour: whole numbers, halves, and a perfect score. */
@SinemaPreview
@Composable
private fun SinemaRatingBadgeRoundingPreview() {
    SinemaPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SinemaRatingBadge(rating = 10.0)
            SinemaRatingBadge(rating = 8.0)
            SinemaRatingBadge(rating = 7.25)
        }
    }
}
