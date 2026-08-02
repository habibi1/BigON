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
import com.bigon.core.designsystem.components.SinemaAttributionFooter
import com.bigon.core.designsystem.components.SinemaCastCard
import com.bigon.core.designsystem.components.SinemaChip
import com.bigon.core.designsystem.components.SinemaEmptyState
import com.bigon.core.designsystem.components.SinemaFavoriteToggle
import com.bigon.core.designsystem.components.SinemaListItem
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.designsystem.components.SinemaMovieCard
import com.bigon.core.designsystem.components.SinemaOfflineBanner
import com.bigon.core.designsystem.components.SinemaPrimaryButton
import com.bigon.core.designsystem.components.SinemaSearchBar
import com.bigon.core.designsystem.components.SinemaSectionHeader
import com.bigon.core.designsystem.components.SinemaSegmentedControl
import com.bigon.core.designsystem.components.SinemaSettingRow
import com.bigon.core.designsystem.components.SinemaShimmerCard
import com.bigon.core.designsystem.components.SinemaSnackbar
import com.bigon.core.designsystem.components.SinemaTonalButton
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * The living style guide: every token and every component on one canvas, in the
 * order of the design spec. Reviewing a theme change means opening this one
 * preview rather than hunting through screens.
 *
 * Rendered tall on purpose — scroll the preview, or open it in Interactive mode.
 */
@SinemaThemePreview
@Composable
private fun SinemaComponentGalleryPreview() {
    SinemaTheme {
        Column(
            modifier = Modifier
                .background(SinemaTheme.colors.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            GalleryHeading("Level 0 · Tokens")
            ColorTokens()
            TypographyTokens()
            SpacingAndShapeTokens()

            GalleryHeading("Level 2 · Content")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SinemaMovieCard(title = "Midnight Reel", meta = "2026 · Thriller", rating = 8.4, onClick = {})
                SinemaMovieCard(title = "No rating variant", meta = "2023 · Documentary", rating = null, onClick = {})
                SinemaShimmerCard()
            }
            SinemaSectionHeader(title = "Trending today", onSeeAll = {})
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SinemaSampleData.cast.take(2).forEach { (name, role) ->
                    SinemaCastCard(name = name, role = role)
                }
            }
            SinemaListItem(
                title = "The Batman II",
                subtitle = "2026 · ★ 7.9 · Crime",
                selected = false,
                onClick = {},
            )
            SinemaListItem(
                title = "Dune: Part Three",
                subtitle = "2026 · ★ 8.4 · Sci-Fi",
                selected = true,
                onClick = {},
            )

            GalleryHeading("Level 2 · Input & control")
            SinemaSearchBar(query = "", onQueryChange = {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SinemaChip(label = "All", selected = true, onClick = {})
                SinemaChip(label = "Sci-Fi", selected = false, onClick = {})
                SinemaChip(label = "Drama", selected = false, onClick = {})
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SinemaPrimaryButton(text = "Watch trailer", leadingIcon = SinemaIcons.Play, onClick = {})
                SinemaTonalButton(text = "Retry", onClick = {})
                SinemaFavoriteToggle(checked = true, onCheckedChange = {})
            }
            SinemaSegmentedControl(
                options = listOf("System", "Dark", "Light"),
                selectedIndex = 1,
                onSelect = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SinemaSettingRow(
                title = "Content language",
                subtitle = "Titles & overviews",
                value = "EN",
                icon = SinemaIcons.Movie,
                onClick = {},
                showDivider = false,
            )

            GalleryHeading("Level 2 · Feedback")
            SinemaOfflineBanner(message = "✈ You're offline — showing saved content")
            SinemaSnackbar(
                message = "Couldn't refresh — showing saved data",
                actionLabel = "RETRY",
                onAction = {},
            )
            SinemaLoadingIndicator(modifier = Modifier.padding(start = 8.dp))
            SinemaEmptyState(
                icon = SinemaIcons.HeartOutline,
                title = "No favorites yet",
                subtitle = "Movies you favorite appear here and work offline.",
                action = { SinemaTonalButton(text = "Browse trending", onClick = {}) },
                modifier = Modifier.fillMaxWidth(),
            )
            SinemaAttributionFooter(versionLabel = "Sinema v0.0.1")
        }
    }
}

// ── gallery scaffolding ─────────────────────────────────────────────────────

@Composable
private fun GalleryHeading(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = text.uppercase(),
            style = SinemaTheme.typography.label,
            color = SinemaTheme.colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SinemaTheme.colors.outline),
        )
    }
}

@Composable
private fun ColorTokens() {
    val colors = SinemaTheme.colors
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
                .clip(SinemaTheme.shapes.card)
                .background(color)
                .border(1.dp, SinemaTheme.colors.outline, SinemaTheme.shapes.card),
        )
        Text(
            text = name,
            style = SinemaTheme.typography.caption,
            color = SinemaTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun TypographyTokens() {
    val type = SinemaTheme.typography
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
            style = SinemaTheme.typography.caption,
            color = SinemaTheme.colors.textSecondary,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = "Dune: Part Three",
            style = style,
            color = SinemaTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun SpacingAndShapeTokens() {
    val spacing = SinemaTheme.spacing
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
            ShapeSample("card", SinemaTheme.shapes.card)
            ShapeSample("container", SinemaTheme.shapes.container)
            ShapeSample("pill", SinemaTheme.shapes.pill)
            ShapeSample("badge", SinemaTheme.shapes.badge)
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
                .background(SinemaTheme.colors.primaryContainer),
        )
        Text(
            text = "$name ${value.value.toInt()}",
            style = SinemaTheme.typography.caption,
            color = SinemaTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun ShapeSample(name: String, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(shape)
            .background(SinemaTheme.colors.surfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = SinemaTheme.typography.caption,
            color = SinemaTheme.colors.textSecondary,
        )
    }
}
