package com.bigon.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.components.BigonChip
import com.bigon.core.designsystem.components.BigonShimmerBox
import com.bigon.core.designsystem.components.BigonEmptyState
import com.bigon.core.designsystem.components.BigonFavoriteToggle
import com.bigon.core.designsystem.components.BigonListItem
import com.bigon.core.designsystem.components.BigonLoadingIndicator
import com.bigon.core.designsystem.components.BigonOfflineBanner
import com.bigon.core.designsystem.components.BigonPrimaryButton
import com.bigon.core.designsystem.components.BigonSearchBar
import com.bigon.core.designsystem.components.BigonSectionHeader
import com.bigon.core.designsystem.components.BigonSegmentedControl
import com.bigon.core.designsystem.components.BigonSettingRow
import com.bigon.core.designsystem.components.BigonSnackbar
import com.bigon.core.designsystem.components.BigonTonalButton
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * The living style guide: every token and every component on one canvas, in the
 * order of the design spec. Reviewing a theme change means opening this one
 * preview rather than hunting through screens.
 *
 * Rendered tall on purpose — scroll the preview, or open it in Interactive mode.
 */
@BigonThemePreview
@Composable
private fun BigonComponentGalleryPreview() {
    BigonTheme {
        Column(
            modifier = Modifier
                .background(BigonTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            GalleryHeading("Level 0 · Tokens")
            ColorTokens()
            TypographyTokens()
            SpacingAndShapeTokens()

            // Domain-shaped components — the movie card, cast card, poster
            // shimmer and TMDB attribution — have their own gallery in
            // :tmdb:ui. This one shows only what any app can use.
            GalleryHeading("Level 2 · Content")
            BigonSectionHeader(title = "Trending today", onSeeAll = {})
            BigonListItem(
                title = "The Batman II",
                subtitle = "2026 · ★ 7.9 · Crime",
                selected = false,
                onClick = {},
                thumbnail = { BigonShimmerBox(modifier = Modifier.matchParentSize()) },
            )
            BigonListItem(
                title = "Dune: Part Three",
                subtitle = "2026 · ★ 8.4 · Sci-Fi",
                selected = true,
                onClick = {},
                thumbnail = { BigonShimmerBox(modifier = Modifier.matchParentSize()) },
            )

            GalleryHeading("Level 2 · Input & control")
            BigonSearchBar(query = "", onQueryChange = {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigonChip(label = "All", selected = true, onClick = {})
                BigonChip(label = "Sci-Fi", selected = false, onClick = {})
                BigonChip(label = "Drama", selected = false, onClick = {})
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BigonPrimaryButton(text = "Watch trailer", leadingIcon = BigonIcons.Play, onClick = {})
                BigonTonalButton(text = "Retry", onClick = {})
                BigonFavoriteToggle(checked = true, onCheckedChange = {})
            }
            BigonSegmentedControl(
                options = listOf("System", "Dark", "Light"),
                selectedIndex = 1,
                onSelect = {},
                modifier = Modifier.fillMaxWidth(),
            )
            BigonSettingRow(
                title = "Content language",
                subtitle = "Titles & overviews",
                value = "EN",
                icon = BigonIcons.Movie,
                onClick = {},
                showDivider = false,
            )

            GalleryHeading("Level 2 · Feedback")
            BigonOfflineBanner(message = "✈ You're offline — showing saved content")
            BigonSnackbar(
                message = "Couldn't refresh — showing saved data",
                actionLabel = "RETRY",
                onAction = {},
            )
            BigonLoadingIndicator(modifier = Modifier.padding(start = 8.dp))
            BigonEmptyState(
                icon = BigonIcons.HeartOutline,
                title = "No favorites yet",
                subtitle = "Movies you favorite appear here and work offline.",
                action = { BigonTonalButton(text = "Browse trending", onClick = {}) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── gallery scaffolding ─────────────────────────────────────────────────────

@Composable
private fun GalleryHeading(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = text.uppercase(),
            style = BigonTheme.typography.label,
            color = BigonTheme.colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BigonTheme.colors.outline),
        )
    }
}

@Composable
private fun ColorTokens() {
    val colors = BigonTheme.colors
    val swatches = listOf(
        "background" to colors.background,
        "surface" to colors.surface,
        "surfaceVariant" to colors.surfaceVariant,
        "surfaceHigh" to colors.surfaceHigh,
        "primary" to colors.primary,
        "primaryContainer" to colors.primaryContainer,
        "textPrimary" to colors.textPrimary,
        "textSecondary" to colors.textSecondary,
        "outline" to colors.outline,
        "errorContainer" to colors.errorContainer,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (name, color) -> Swatch(name, color) }
            }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(
        modifier = Modifier.width(84.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(BigonTheme.shapes.card)
                .background(color)
                .border(1.dp, BigonTheme.colors.outline, BigonTheme.shapes.card),
        )
        Text(
            text = name,
            style = BigonTheme.typography.caption,
            color = BigonTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun TypographyTokens() {
    val type = BigonTheme.typography
    val rows = listOf(
        "display" to type.display,
        "title" to type.title,
        "cardTitle" to type.cardTitle,
        "body" to type.body,
        "label" to type.label,
        "caption" to type.caption,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (name, style) -> TypeRow(name, style) }
    }
}

@Composable
private fun TypeRow(name: String, style: TextStyle) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = name,
            style = BigonTheme.typography.caption,
            color = BigonTheme.colors.textSecondary,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = "Dune: Part Three",
            style = style,
            color = BigonTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun SpacingAndShapeTokens() {
    val spacing = BigonTheme.spacing
    val steps = listOf(
        "xs" to spacing.xs,
        "s" to spacing.s,
        "m" to spacing.m,
        "l" to spacing.l,
        "xl" to spacing.xl,
        "xxl" to spacing.xxl,
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            steps.forEach { (name, value) -> SpacingStep(name, value) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShapeSample("card", BigonTheme.shapes.card)
            ShapeSample("container", BigonTheme.shapes.container)
            ShapeSample("pill", BigonTheme.shapes.pill)
            ShapeSample("badge", BigonTheme.shapes.badge)
        }
    }
}

@Composable
private fun SpacingStep(name: String, value: Dp) {
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

@Composable
private fun ShapeSample(name: String, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .size(58.dp)
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
