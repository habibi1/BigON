package com.bigon.tmdb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.bigon.core.designsystem.components.BigonShimmerBox
import com.bigon.core.designsystem.components.BigonShimmerText
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * BigonShimmerCard — the loading variant of [BigonMovieCard]
 * (component gallery §Content). Same footprint, animated sheen.
 */
@Composable
fun BigonShimmerCard(
    modifier: Modifier = Modifier,
    width: Dp = BigonMovieCardDefaults.Width,
) {
    // Dp.Unspecified lets a parent (e.g. a grid cell) size the skeleton, exactly
    // as BigonMovieCard allows — the two must stay interchangeable.
    Column(modifier = modifier.then(if (width.isSpecified) Modifier.width(width) else Modifier)) {
        BigonShimmerBox(
            shape = BigonTheme.shapes.card,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BigonMovieCardDefaults.PosterAspectRatio),
        )
        // Two title lines and one meta line, sized from the styles the real
        // card uses and padded the same way, so the skeleton reserves the
        // height the card will take rather than an approximation of it.
        // Measured: the old fixed 12dp/10dp bars left a recommendations row
        // 46px short — one wrapped title line — and every section below it
        // moved when the row filled in. BigonMovieCard allows the title two
        // lines and most film titles use both at this width.
        BigonShimmerText(
            style = BigonTheme.typography.cardTitle,
            lines = 2,
            lastLineFraction = 0.6f,
            modifier = Modifier.padding(top = BigonTheme.spacing.s),
        )
        BigonShimmerText(
            style = BigonTheme.typography.caption,
            lastLineFraction = 0.55f,
            modifier = Modifier.padding(top = BigonTheme.spacing.xs / 2),
        )
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonShimmerCardPreview() {
    BigonPreviewSurface {
        BigonShimmerCard()
    }
}

/** A loading row, as Home renders it before the feed arrives. */
@BigonPreview
@Composable
private fun BigonShimmerCardRowPreview() {
    BigonPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) { BigonShimmerCard() }
        }
    }
}

@BigonPreview
@Composable
private fun BigonShimmerCardWidePreview() {
    BigonPreviewSurface {
        BigonShimmerCard(width = 180.dp)
    }
}

/** Parent-sized skeleton — must stay layout-compatible with BigonMovieCard. */
@BigonPreview
@Composable
private fun BigonShimmerCardParentSizedPreview() {
    BigonPreviewSurface {
        BigonShimmerCard(width = Dp.Unspecified, modifier = Modifier.width(200.dp))
    }
}
