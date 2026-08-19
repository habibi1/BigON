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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview

/**
 * Level 0 token — BigonSpacing: "the only dp values features may use"
 * (component gallery §Tokens).
 */
@Immutable
data class BigonSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
)

internal val LocalSinemaSpacing = staticCompositionLocalOf { BigonSpacing() }

// ── Previews ────────────────────────────────────────────────────────────────

/** The spacing scale — the only dp values features may use. */
@BigonThemePreview
@Composable
private fun BigonSpacingPreview() {
    val s = BigonTheme.spacing
    val steps = listOf("xs" to s.xs, "s" to s.s, "m" to s.m, "l" to s.l, "xl" to s.xl, "xxl" to s.xxl)
    BigonPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            steps.forEach { (name, value) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(value)
                            .background(BigonTheme.colors.primaryContainer),
                    )
                    Text(
                        text = "$name ${value.value.toInt()}",
                        style = BigonTheme.typography.caption,
                        color = BigonTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}
