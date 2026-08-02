package com.bigon.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaDevicePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/** One top-level destination in the app shell. */
data class SinemaNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * SinemaAppScaffold — the navigation shell. Compact widths get a bottom bar,
 * medium/expanded widths (≥600dp, the Material window-size-class boundary) get
 * a navigation rail. The adaptation happens INSIDE the component (F6.2):
 * screens never branch on device type.
 *
 * The scaffold consumes the system-bar insets on the navigation side; content
 * is responsible for its own top inset (screens usually start with a header
 * that applies statusBarsPadding, or scroll under it deliberately).
 */
@Composable
fun SinemaAppScaffold(
    items: List<SinemaNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Full-bleed destinations (detail, media) hide the bar entirely. */
    showNavigation: Boolean = true,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SinemaTheme.colors.background),
    ) {
        val useRail = maxWidth >= 600.dp

        // One structure for every combination of width and [showNavigation]:
        // content always sits at Row → Column → Box. Swapping content between
        // different parents would relocate its subtree and silently discard
        // saved state — scroll positions reset on the way back from a
        // full-bleed screen.
        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && showNavigation) {
                SinemaNavigationRail(items, selectedId, onSelect)
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f)) { content() }
                if (!useRail && showNavigation) {
                    SinemaBottomBar(items, selectedId, onSelect)
                }
            }
        }
    }
}

@Composable
private fun SinemaBottomBar(
    items: List<SinemaNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SinemaTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .height(72.dp),
    ) {
        items.forEach { item ->
            SinemaNavSlot(
                item = item,
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SinemaNavigationRail(
    items: List<SinemaNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.xs),
        modifier = Modifier
            .fillMaxHeight()
            .background(SinemaTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .width(80.dp)
            .padding(vertical = 14.dp),
    ) {
        items.forEach { item ->
            SinemaNavSlot(
                item = item,
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SinemaTheme.spacing.s),
            )
        }
    }
}

@Composable
private fun SinemaNavSlot(
    item: SinemaNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SinemaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(SinemaTheme.shapes.container)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 28.dp)
                .clip(SinemaTheme.shapes.pill)
                .background(if (selected) colors.primaryContainer else Color.Transparent),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) colors.onPrimaryContainer else colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = item.label,
            style = SinemaTheme.typography.caption.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

private val previewNavItems = listOf(
    SinemaNavItem("home", "Home", SinemaIcons.Home),
    SinemaNavItem("search", "Search", SinemaIcons.Search),
    SinemaNavItem("favorites", "Favorites", SinemaIcons.HeartOutline),
    SinemaNavItem("settings", "Settings", SinemaIcons.Settings),
)

@Composable
private fun PreviewScaffold(selectedId: String = "home") {
    var selected by remember { mutableStateOf(selectedId) }
    SinemaTheme {
        SinemaAppScaffold(
            items = previewNavItems,
            selectedId = selected,
            onSelect = { selected = it },
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Screen content",
                    style = SinemaTheme.typography.title,
                    color = SinemaTheme.colors.textSecondary,
                )
            }
        }
    }
}

/** Compact width: bottom bar. */
@Preview(name = "Compact · bottom bar", widthDp = 411, heightDp = 480, showBackground = true)
@Composable
private fun SinemaAppScaffoldCompactPreview() = PreviewScaffold()

/** Expanded width: the same call renders a navigation rail instead. */
@Preview(name = "Expanded · rail", widthDp = 840, heightDp = 480, showBackground = true)
@Composable
private fun SinemaAppScaffoldExpandedPreview() = PreviewScaffold()

/** The 600dp switch point, one dp either side. */
@Preview(name = "599dp · bar", widthDp = 599, heightDp = 420, showBackground = true)
@Preview(name = "600dp · rail", widthDp = 600, heightDp = 420, showBackground = true)
@Composable
private fun SinemaAppScaffoldBreakpointPreview() = PreviewScaffold()

/** A non-first destination selected, to check the indicator moves. */
@SinemaPreview
@Composable
private fun SinemaAppScaffoldSelectionPreview() = PreviewScaffold(selectedId = "favorites")

@SinemaDevicePreview
@Composable
private fun SinemaAppScaffoldDevicePreview() = PreviewScaffold()

@SinemaThemePreview
@Composable
private fun SinemaAppScaffoldThemePreview() = PreviewScaffold()
