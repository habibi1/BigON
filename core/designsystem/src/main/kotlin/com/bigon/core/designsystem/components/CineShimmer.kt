package com.bigon.core.designsystem.components

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
import com.bigon.core.designsystem.preview.CinePreview
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineThemePreview
import com.bigon.core.designsystem.theme.CineTheme

/**
 * CineShimmerCard — the loading variant of [CineMovieCard]
 * (component gallery §Content). Same footprint, animated sheen.
 */
@Composable
fun CineShimmerCard(
    modifier: Modifier = Modifier,
    width: Dp = CineMovieCardDefaults.Width,
) {
    // Dp.Unspecified lets a parent (e.g. a grid cell) size the skeleton, exactly
    // as CineMovieCard allows — the two must stay interchangeable.
    Column(modifier = modifier.then(if (width.isSpecified) Modifier.width(width) else Modifier)) {
        CineShimmerBox(
            shape = CineTheme.shapes.card,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CineMovieCardDefaults.PosterAspectRatio),
        )
        CineShimmerBox(
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .padding(top = CineTheme.spacing.s)
                .width(100.dp)
                .height(12.dp),
        )
        CineShimmerBox(
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .padding(top = 5.dp)
                .width(64.dp)
                .height(10.dp),
        )
    }
}

/** Building block for any skeleton layout: an animated shimmering surface. */
@Composable
fun CineShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = CineTheme.shapes.card,
) {
    val base = CineTheme.colors.surfaceVariant
    val highlight = CineTheme.colors.surfaceHigh

    val transition = rememberInfiniteTransition(label = "cineShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "cineShimmerSweep",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(base)
                val sweepWidth = size.width
                val start = (progress * 2f - 1f) * (size.width + sweepWidth)
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(start, 0f),
                        end = Offset(start + sweepWidth, size.height),
                    ),
                )
            },
    )
}

// ── Previews ────────────────────────────────────────────────────────────────

@CineThemePreview
@Composable
private fun CineShimmerCardPreview() {
    CinePreviewSurface {
        CineShimmerCard()
    }
}

/** A loading row, as Home renders it before the feed arrives. */
@CinePreview
@Composable
private fun CineShimmerCardRowPreview() {
    CinePreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) { CineShimmerCard() }
        }
    }
}

@CinePreview
@Composable
private fun CineShimmerCardWidePreview() {
    CinePreviewSurface {
        CineShimmerCard(width = 180.dp)
    }
}

/** The primitive on its own: thumbnail block and text lines. */
@CineThemePreview
@Composable
private fun CineShimmerBoxPreview() {
    CinePreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CineShimmerBox(modifier = Modifier.width(52.dp).height(78.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CineShimmerBox(modifier = Modifier.width(160.dp).height(14.dp))
                CineShimmerBox(modifier = Modifier.width(96.dp).height(12.dp))
            }
        }
    }
}

/** Parent-sized skeleton — must stay layout-compatible with CineMovieCard. */
@CinePreview
@Composable
private fun CineShimmerCardParentSizedPreview() {
    CinePreviewSurface {
        CineShimmerCard(width = Dp.Unspecified, modifier = Modifier.width(200.dp))
    }
}
