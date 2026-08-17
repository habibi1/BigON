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

/**
 * The same scale, re-cut for three metres away.
 *
 * A phone is read at roughly 40cm and a television at 3m, so the type has to
 * grow by something close to that ratio to subtend the same angle. The scale
 * below is not a uniform multiplier: the small end grows hardest, because
 * 11sp metadata is what disappears first and a 24sp title was already legible.
 * The relationships between the steps are preserved so components keep their
 * hierarchy without being re-laid-out.
 *
 * Kept as a separate factory rather than a scale factor on [BigonTypography]
 * so the values are readable as a set — a multiplier hides which line is the
 * one that was actually failing.
 */
fun televisionTypography(): BigonTypography = BigonTypography(
    display = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    title = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    cardTitle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp,
    ),
    body = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
    label = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
    ),
    caption = TextStyle(fontSize = 14.sp),
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
