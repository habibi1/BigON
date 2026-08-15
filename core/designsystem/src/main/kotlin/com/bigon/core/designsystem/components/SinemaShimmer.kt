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
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme


/** Building block for any skeleton layout: an animated shimmering surface. */
@Composable
fun SinemaShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = SinemaTheme.shapes.card,
) {
    val base = SinemaTheme.colors.surfaceVariant
    val highlight = SinemaTheme.colors.surfaceHigh

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




/** The primitive on its own: thumbnail block and text lines. */
@SinemaThemePreview
@Composable
private fun SinemaShimmerBoxPreview() {
    SinemaPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SinemaShimmerBox(modifier = Modifier.width(52.dp).height(78.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SinemaShimmerBox(modifier = Modifier.width(160.dp).height(14.dp))
                SinemaShimmerBox(modifier = Modifier.width(96.dp).height(12.dp))
            }
        }
    }
}

