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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonColors
import com.bigon.core.designsystem.theme.BigonTheme


/**
 * The two tones a shimmer sweeps between: the resting surface and the sheen
 * that travels across it.
 *
 * A parameter rather than a constant because a placeholder has to belong to
 * whatever it sits on. The default greys are right on the app background and
 * wrong inside a selected chip, which is gold — a grey bar there reads as a
 * different component rather than as that chip still loading.
 */
@Immutable
data class BigonShimmerColors(val base: Color, val highlight: Color)

object BigonShimmerDefaults {
    /** Placeholders on the page itself. */
    val colors: BigonShimmerColors
        @Composable get() = BigonShimmerColors(
            base = BigonTheme.colors.surfaceVariant,
            highlight = BigonTheme.colors.surfaceHigh,
        )

    /**
     * Placeholders drawn *on* [BigonColors.primaryContainer] — a selected chip.
     * Derived from the container's own foreground at low alpha, so it stays a
     * tint of that surface in either theme rather than a second colour.
     */
    val onPrimaryContainer: BigonShimmerColors
        @Composable get() = BigonShimmerColors(
            base = BigonTheme.colors.onPrimaryContainer.copy(alpha = 0.22f),
            highlight = BigonTheme.colors.onPrimaryContainer.copy(alpha = 0.45f),
        )
}

/** Building block for any skeleton layout: an animated shimmering surface. */
@Composable
fun BigonShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = BigonTheme.shapes.card,
    colors: BigonShimmerColors = BigonShimmerDefaults.colors,
) {
    val base = colors.base
    val highlight = colors.highlight

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

/**
 * Shimmer standing in for text that has not arrived, sized from the very
 * [TextStyle] the real [Text] will use.
 *
 * The height is measured rather than guessed. A skeleton built from hardcoded
 * heights agrees with the text it replaces exactly once — on the day it is
 * written — and drifts silently the next time the type scale moves. Measuring
 * the style means the placeholder always occupies the line box the text is
 * about to, which is the whole point of a skeleton.
 *
 * [lines] and [lastLineFraction] describe a paragraph: the closing line of real
 * prose stops short of the margin, and a block of full-width bars reads as a
 * table rather than as text.
 */
@Composable
fun BigonShimmerText(
    style: TextStyle,
    modifier: Modifier = Modifier,
    lines: Int = 1,
    lastLineFraction: Float = 1f,
    placeholderFor: String? = null,
    colors: BigonShimmerColors = BigonShimmerDefaults.colors,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    // "Ag" spans an ascender and a descender, so the measured box is the full
    // line box rather than the height of whichever glyphs happened to be used.
    val lineHeight = remember(style, density, measurer) {
        with(density) { measurer.measure(AnnotatedString("Ag"), style).size.height.toDp() }
    }
    // The bar is the glyph band, not the whole line box; the leftover leading
    // becomes the gap, so N bars plus N-1 gaps come to N lines of real text.
    val barHeight = remember(style, density, lineHeight) {
        if (style.fontSize.isSpecified) with(density) { style.fontSize.toDp() } else lineHeight
    }

    // A sample value sizes the bar to the width that value would render at, so
    // a placeholder for a runtime is as wide as "1h 50m" rather than as wide as
    // whatever fraction looked about right.
    val sampleWidth = remember(placeholderFor, style, density, measurer) {
        placeholderFor?.let {
            with(density) { measurer.measure(AnnotatedString(it), style).size.width.toDp() }
        }
    }

    Column(modifier = modifier) {
        repeat(lines) { index ->
            // Each bar sits inside a box of the real line height, rather than
            // being spaced apart by the leading. Same visual result, but the
            // block comes to exactly `lines` line boxes — spacing between bars
            // leaves the last line's leading out, and every section below
            // inherits the error.
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .then(if (sampleWidth != null) Modifier.width(sampleWidth) else Modifier.fillMaxWidth())
                    .height(lineHeight),
            ) {
                BigonShimmerBox(
                    shape = BigonTheme.shapes.pill,
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth(if (index == lines - 1) lastLineFraction else 1f)
                        .height(barHeight),
                )
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────




/** The primitive on its own: thumbnail block and text lines. */
@BigonThemePreview
@Composable
private fun BigonShimmerBoxPreview() {
    BigonPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigonShimmerBox(modifier = Modifier.width(52.dp).height(78.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BigonShimmerBox(modifier = Modifier.width(160.dp).height(14.dp))
                BigonShimmerBox(modifier = Modifier.width(96.dp).height(12.dp))
            }
        }
    }
}

