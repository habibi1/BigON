package com.bigon.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview

/** Level 0 token — BigonShape: card 12 · container 16 · pill (component gallery §Tokens). */
@Immutable
data class BigonShapes(
    /** Posters, list thumbnails, snackbars. */
    val card: Shape = RoundedCornerShape(12.dp),
    /** Larger containers — spec cards, nav bar. */
    val container: Shape = RoundedCornerShape(16.dp),
    /** Buttons, chips, search bar, toggles. */
    val pill: Shape = CircleShape,
    /** Small elements — rating badge, banner. */
    val badge: Shape = RoundedCornerShape(8.dp),
)

internal val LocalSinemaShapes = staticCompositionLocalOf { BigonShapes() }

// ── Previews ────────────────────────────────────────────────────────────────

/** Corner treatments: card, container, pill, badge. */
@BigonThemePreview
@Composable
private fun BigonShapesPreview() {
    val sh = BigonTheme.shapes
    val shapes = listOf("card" to sh.card, "container" to sh.container, "pill" to sh.pill, "badge" to sh.badge)
    BigonPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            shapes.forEach { (name, shape) ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(shape)
                        .background(BigonTheme.colors.surfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name,
                        style = BigonTheme.typography.caption,
                        color = BigonTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}
