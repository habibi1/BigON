package com.bigon.core.designsystem.icons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * The design system's icon vocabulary, backed by Material icons. Components and
 * features reference these named roles — never `Icons.*` directly — so swapping
 * the icon set (or adding brand icons) stays a one-file change.
 */
object BigonIcons {
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Home: ImageVector = Icons.Outlined.Home
    val Search: ImageVector = Icons.Outlined.Search
    val Settings: ImageVector = Icons.Outlined.Settings
    val Heart: ImageVector = Icons.Filled.Favorite
    val HeartOutline: ImageVector = Icons.Outlined.FavoriteBorder
    val Star: ImageVector = Icons.Filled.Star
    val Clear: ImageVector = Icons.Filled.Clear
    val Play: ImageVector = Icons.Filled.PlayArrow
    val Person: ImageVector = Icons.Filled.Person
    val ChevronRight: ImageVector = Icons.Filled.ChevronRight
    val Movie: ImageVector = Icons.Filled.Movie
}

// ── Previews ────────────────────────────────────────────────────────────────

private val iconSheet = listOf(
    "Back" to BigonIcons.Back,
    "Home" to BigonIcons.Home,
    "Search" to BigonIcons.Search,
    "Settings" to BigonIcons.Settings,
    "Heart" to BigonIcons.Heart,
    "HeartOutline" to BigonIcons.HeartOutline,
    "Star" to BigonIcons.Star,
    "Clear" to BigonIcons.Clear,
    "Play" to BigonIcons.Play,
    "Person" to BigonIcons.Person,
    "ChevronRight" to BigonIcons.ChevronRight,
    "Movie" to BigonIcons.Movie,
)

/** The full icon vocabulary — the sheet to check before adding a new glyph. */
@BigonThemePreview
@Composable
private fun BigonIconsPreview() {
    BigonPreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            iconSheet.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { (name, icon) ->
                        Column(
                            modifier = Modifier.width(72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = BigonTheme.colors.textPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = name,
                                style = BigonTheme.typography.caption,
                                color = BigonTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Rendering sizes actually used by components: 11dp badge → 30dp placeholder. */
@BigonPreview
@Composable
private fun BigonIconSizesPreview() {
    BigonPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(11.dp, 16.dp, 18.dp, 20.dp, 24.dp, 30.dp).forEach { size ->
                Icon(
                    imageVector = BigonIcons.Star,
                    contentDescription = null,
                    tint = BigonTheme.colors.primary,
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}
