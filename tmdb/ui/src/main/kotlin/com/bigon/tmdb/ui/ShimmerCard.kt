package com.bigon.tmdb.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.bigon.core.designsystem.components.BigonShimmerBox
import com.bigon.core.designsystem.icons.BigonIcons
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
        BigonShimmerBox(
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .padding(top = BigonTheme.spacing.s)
                .width(100.dp)
                .height(12.dp),
        )
        BigonShimmerBox(
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .padding(top = 5.dp)
                .width(64.dp)
                .height(10.dp),
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
