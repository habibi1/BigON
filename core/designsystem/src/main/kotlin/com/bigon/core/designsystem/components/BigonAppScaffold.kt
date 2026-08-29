package com.bigon.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonDevicePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/** One top-level destination in the app shell. */
data class BigonNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Matches the fade the navigation host uses between destinations, so the bar
 * and the screen behind it move together instead of one chasing the other.
 */
private val NAV_FADE = tween<Float>(durationMillis = 220)

/** The bar's own height, before the gesture inset underneath it. */
private val BOTTOM_BAR_HEIGHT = 72.dp

/** The rail's own width, before the display-cutout inset beside it. */
private val RAIL_WIDTH = 80.dp

/**
 * BigonAppScaffold — the navigation shell. Compact widths get a bottom bar,
 * medium/expanded widths (≥600dp, the Material window-size-class boundary) get
 * a navigation rail. The adaptation happens INSIDE the component (F6.2):
 * screens never branch on device type.
 *
 * The scaffold consumes the system-bar insets on the navigation side; content
 * is responsible for its own top inset (screens usually start with a header
 * that applies statusBarsPadding, or scroll under it deliberately).
 */
@Composable
fun BigonAppScaffold(
    items: List<BigonNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Full-bleed destinations (detail, media) hide the bar entirely. */
    showNavigation: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BigonTheme.colors.background),
    ) {
        val useRail = maxWidth >= 600.dp
        val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

        // Content fills the window whatever the width class and whether or not
        // navigation is showing; the bar and the rail float over it. Nothing
        // resizes as navigation comes and goes, which is the whole point — see
        // [FloatingBottomBar].
        //
        // How much of the window each one covers is therefore constant for a
        // given width class rather than tied to [showNavigation]. Screens hold
        // their content clear of it; a full-bleed screen ignores it and runs
        // underneath, the same way the top of the app runs under the status bar.
        val navigationOverlap = if (useRail) {
            0.dp
        } else {
            BOTTOM_BAR_HEIGHT + safeDrawing.calculateBottomPadding()
        }
        val railOverlap = if (useRail) {
            RAIL_WIDTH + safeDrawing.calculateStartPadding(LocalLayoutDirection.current)
        } else {
            0.dp
        }

        // One structure for every combination: content always sits directly in
        // this Box. Swapping it between different parents would relocate its
        // subtree and silently discard saved state — scroll positions reset on
        // the way back from a full-bleed screen — and unfolding the device
        // crosses the width boundary while the app is running.
        //
        // Navigation is composed after content, so it draws over it. That is
        // also what keeps a flying shared element underneath the bar: the
        // overlay belongs to the transition layout inside the content slot.
        Box(modifier = Modifier.fillMaxSize()) {
            content(PaddingValues(start = railOverlap, bottom = navigationOverlap))
            FloatingNavigationRail(
                visible = useRail && showNavigation,
                items = items,
                selectedId = selectedId,
                onSelect = onSelect,
            )
            FloatingBottomBar(
                visible = !useRail && showNavigation,
                items = items,
                selectedId = selectedId,
                onSelect = onSelect,
            )
        }
    }
}

/**
 * The rail, floating over content that already fills the window.
 *
 * It used to be a Row sibling, which measured badly on the way out: a slide
 * translates without giving up its slot, so an 80dp column of bare background
 * sat where the rail had been for the length of the animation and the whole
 * screen jumped left the frame it finished. Measured on a fold-open emulator,
 * three consecutive frames had the leftmost 195px at the background colour
 * while content still started at x=200.
 *
 * A BoxScope extension for the same reason as [FloatingBottomBar]: `align`.
 */
@Composable
private fun BoxScope.FloatingNavigationRail(
    visible: Boolean,
    items: List<BigonNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.CenterStart),
        enter = slideInHorizontally { -it } + fadeIn(NAV_FADE),
        exit = slideOutHorizontally { -it } + fadeOut(NAV_FADE),
    ) {
        BigonNavigationRail(items, selectedId, onSelect)
    }
}

/**
 * The bar, floating over content that already fills the window — the same way
 * the top of the app runs under the status bar.
 *
 * It used to be a sibling that took its own space, which meant the content area
 * grew and shrank as navigation came and went; on the way out that left a blank
 * band for the length of the animation, because the incoming screen had not
 * painted there yet. Nothing resizes now, so the bar leaves as smoothly as it
 * arrives.
 *
 * Its own composable so that `align` — and so BoxScope — is in scope where it
 * is needed, without the shell having to be a Box-inside-something-else.
 */
@Composable
private fun BoxScope.FloatingBottomBar(
    visible: Boolean,
    items: List<BigonNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it } + fadeIn(NAV_FADE),
        exit = slideOutVertically { it } + fadeOut(NAV_FADE),
    ) {
        BigonBottomBar(items, selectedId, onSelect)
    }
}

@Composable
private fun BigonBottomBar(
    items: List<BigonNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BigonTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .height(BOTTOM_BAR_HEIGHT),
    ) {
        items.forEach { item ->
            BigonNavSlot(
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
private fun BigonNavigationRail(
    items: List<BigonNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BigonTheme.spacing.xs),
        modifier = Modifier
            .fillMaxHeight()
            .background(BigonTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .width(RAIL_WIDTH)
            .padding(vertical = 14.dp),
    ) {
        items.forEach { item ->
            BigonNavSlot(
                item = item,
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BigonTheme.spacing.s),
            )
        }
    }
}

@Composable
private fun BigonNavSlot(
    item: BigonNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BigonTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(BigonTheme.shapes.container)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 28.dp)
                .clip(BigonTheme.shapes.pill)
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
            style = BigonTheme.typography.caption.copy(
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
    BigonNavItem("home", "Home", BigonIcons.Home),
    BigonNavItem("search", "Search", BigonIcons.Search),
    BigonNavItem("favorites", "Favorites", BigonIcons.HeartOutline),
    BigonNavItem("settings", "Settings", BigonIcons.Settings),
)

@Composable
private fun PreviewScaffold(selectedId: String = "home") {
    var selected by remember { mutableStateOf(selectedId) }
    BigonTheme {
        BigonAppScaffold(
            items = previewNavItems,
            selectedId = selected,
            onSelect = { selected = it },
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Screen content",
                    style = BigonTheme.typography.title,
                    color = BigonTheme.colors.textSecondary,
                )
            }
        }
    }
}

/** Compact width: bottom bar. */
@Preview(name = "Compact · bottom bar", widthDp = 411, heightDp = 480, showBackground = true)
@Composable
private fun BigonAppScaffoldCompactPreview() = PreviewScaffold()

/** Expanded width: the same call renders a navigation rail instead. */
@Preview(name = "Expanded · rail", widthDp = 840, heightDp = 480, showBackground = true)
@Composable
private fun BigonAppScaffoldExpandedPreview() = PreviewScaffold()

/** The 600dp switch point, one dp either side. */
@Preview(name = "599dp · bar", widthDp = 599, heightDp = 420, showBackground = true)
@Preview(name = "600dp · rail", widthDp = 600, heightDp = 420, showBackground = true)
@Composable
private fun BigonAppScaffoldBreakpointPreview() = PreviewScaffold()

/** A non-first destination selected, to check the indicator moves. */
@BigonPreview
@Composable
private fun BigonAppScaffoldSelectionPreview() = PreviewScaffold(selectedId = "favorites")

@BigonDevicePreview
@Composable
private fun BigonAppScaffoldDevicePreview() = PreviewScaffold()

@BigonThemePreview
@Composable
private fun BigonAppScaffoldThemePreview() = PreviewScaffold()
