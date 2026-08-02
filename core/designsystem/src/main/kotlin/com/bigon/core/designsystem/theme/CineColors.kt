package com.bigon.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineThemePreview

/**
 * Level 0 token — CineColors (component gallery §Tokens).
 *
 * Features never reference raw colors; they read roles from [CineTheme.colors].
 * The dark/light sets below recolor every component with zero component changes.
 */
@Immutable
data class CineColors(
    val background: Color,        // Night900 — window background
    val surface: Color,           // Night800 — cards, bars
    val surfaceVariant: Color,    // Night700 — search bar, shimmer base
    val surfaceHigh: Color,       // Night600 — chips, tonal buttons
    val primary: Color,           // Amber500
    val onPrimary: Color,
    val primaryContainer: Color,  // AmberDim
    val onPrimaryContainer: Color,
    val textPrimary: Color,       // Mist100
    val textSecondary: Color,     // Mist400
    val outline: Color,           // Line
    val errorContainer: Color,
    val onErrorContainer: Color,
    val rating: Color,            // rating badge stars/text
    val favoriteActive: Color,    // active heart in CineFavoriteToggle
    val cardBorder: Color,
    val isDark: Boolean,
)

val CineDarkColors = CineColors(
    background = Color(0xFF101418),
    surface = Color(0xFF1A2026),
    surfaceVariant = Color(0xFF232B33),
    surfaceHigh = Color(0xFF2C3640),
    primary = Color(0xFFFFB648),
    onPrimary = Color(0xFF3F2B00),
    primaryContainer = Color(0xFF5A4200),
    onPrimaryContainer = Color(0xFFFFB648),
    textPrimary = Color(0xFFE8ECEF),
    textSecondary = Color(0xFF9AA6B0),
    outline = Color(0xFF3A444E),
    errorContainer = Color(0xFF402A2A),
    onErrorContainer = Color(0xFFFFB4AB),
    rating = Color(0xFFFFB648),
    favoriteActive = Color(0xFFFF6B81),
    cardBorder = Color(0xFF232B33),
    isDark = true,
)

val CineLightColors = CineColors(
    background = Color(0xFFFAF9F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1EEE9),
    surfaceHigh = Color(0xFFE7E2DA),
    primary = Color(0xFF8A5A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF5A3C00),
    textPrimary = Color(0xFF1C1B18),
    textSecondary = Color(0xFF5E5A52),
    outline = Color(0xFFD5CFC4),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    rating = Color(0xFF8A5A00),
    favoriteActive = Color(0xFFFF6B81),
    cardBorder = Color(0xFFE0DBD2),
    isDark = false,
)

internal val LocalCineColors = staticCompositionLocalOf { CineDarkColors }

// ── Previews ────────────────────────────────────────────────────────────────

/** Every colour role, in both themes — the check after any palette edit. */
@CineThemePreview
@Composable
private fun CineColorsPreview() {
    val c = CineTheme.colors
    val roles = listOf(
        "background" to c.background,
        "surface" to c.surface,
        "surfaceVariant" to c.surfaceVariant,
        "surfaceHigh" to c.surfaceHigh,
        "primary" to c.primary,
        "onPrimary" to c.onPrimary,
        "primaryContainer" to c.primaryContainer,
        "onPrimaryContainer" to c.onPrimaryContainer,
        "textPrimary" to c.textPrimary,
        "textSecondary" to c.textSecondary,
        "outline" to c.outline,
        "errorContainer" to c.errorContainer,
        "onErrorContainer" to c.onErrorContainer,
        "rating" to c.rating,
        "favoriteActive" to c.favoriteActive,
        "cardBorder" to c.cardBorder,
    )
    CinePreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            roles.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (name, color) ->
                        Column(
                            modifier = Modifier.width(84.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(CineTheme.shapes.card)
                                    .background(color)
                                    .border(1.dp, CineTheme.colors.outline, CineTheme.shapes.card),
                            )
                            Text(
                                text = name,
                                style = CineTheme.typography.caption,
                                color = CineTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
