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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview

/** Level 0 token — BigonTypography (component gallery §Tokens). */
@Immutable
data class BigonTypography(
    /** Screen-level wordmark/headers — "Sinema". */
    val display: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    /** Section titles — "Trending today". */
    val title: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    /** Movie card titles. */
    val cardTitle: TextStyle = TextStyle(
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
    ),
    /** Overview/body copy. */
    val body: TextStyle = TextStyle(fontSize = 13.5.sp, lineHeight = 21.sp),
    /** Buttons, "SEE ALL" affordances. */
    val label: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
    ),
    /** Metadata — "2026 · Sci-Fi". */
    val caption: TextStyle = TextStyle(fontSize = 11.sp),
)

internal val LocalSinemaTypography = staticCompositionLocalOf { BigonTypography() }

// ── Previews ────────────────────────────────────────────────────────────────

/** The type scale, each style labelled with its token name. */
@BigonThemePreview
@Composable
private fun BigonTypographyPreview() {
    val t = BigonTheme.typography
    val scale = listOf(
        "display" to t.display,
        "title" to t.title,
        "cardTitle" to t.cardTitle,
        "body" to t.body,
        "label" to t.label,
        "caption" to t.caption,
    )
    BigonPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            scale.forEach { (name, style) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = name,
                        style = BigonTheme.typography.caption,
                        color = BigonTheme.colors.textSecondary,
                        modifier = Modifier.width(76.dp),
                    )
                    Text(
                        text = "Dune: Part Three",
                        style = style,
                        color = BigonTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}
