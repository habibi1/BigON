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
import androidx.compose.material.icons.filled.Tune
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
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * The design system's icon vocabulary, backed by Material icons. Components and
 * features reference these named roles — never `Icons.*` directly — so swapping
 * the icon set (or adding brand icons) stays a one-file change.
 */
object SinemaIcons {
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

    /** Opens a refinement sheet — sliders, not a funnel: these adjust, not exclude. */
    val Filter: ImageVector = Icons.Filled.Tune
}

// ── Previews ────────────────────────────────────────────────────────────────

private val iconSheet = listOf(
    "Back" to SinemaIcons.Back,
    "Home" to SinemaIcons.Home,
    "Search" to SinemaIcons.Search,
    "Settings" to SinemaIcons.Settings,
    "Heart" to SinemaIcons.Heart,
    "HeartOutline" to SinemaIcons.HeartOutline,
    "Star" to SinemaIcons.Star,
    "Clear" to SinemaIcons.Clear,
    "Play" to SinemaIcons.Play,
    "Person" to SinemaIcons.Person,
    "ChevronRight" to SinemaIcons.ChevronRight,
    "Movie" to SinemaIcons.Movie,
    "Filter" to SinemaIcons.Filter,
)

/** The full icon vocabulary — the sheet to check before adding a new glyph. */
@SinemaThemePreview
@Composable
private fun SinemaIconsPreview() {
    SinemaPreviewSurface {
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
                                tint = SinemaTheme.colors.textPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = name,
                                style = SinemaTheme.typography.caption,
                                color = SinemaTheme.colors.textSecondary,
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
@SinemaPreview
@Composable
private fun SinemaIconSizesPreview() {
    SinemaPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(11.dp, 16.dp, 18.dp, 20.dp, 24.dp, 30.dp).forEach { size ->
                Icon(
                    imageVector = SinemaIcons.Star,
                    contentDescription = null,
                    tint = SinemaTheme.colors.primary,
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}
