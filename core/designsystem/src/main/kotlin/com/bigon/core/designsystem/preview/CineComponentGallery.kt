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
import com.bigon.core.designsystem.components.CineAttributionFooter
import com.bigon.core.designsystem.components.CineCastCard
import com.bigon.core.designsystem.components.CineChip
import com.bigon.core.designsystem.components.CineEmptyState
import com.bigon.core.designsystem.components.CineFavoriteToggle
import com.bigon.core.designsystem.components.CineListItem
import com.bigon.core.designsystem.components.CineLoadingIndicator
import com.bigon.core.designsystem.components.CineMovieCard
import com.bigon.core.designsystem.components.CineOfflineBanner
import com.bigon.core.designsystem.components.CinePrimaryButton
import com.bigon.core.designsystem.components.CineSearchBar
import com.bigon.core.designsystem.components.CineSectionHeader
import com.bigon.core.designsystem.components.CineSegmentedControl
import com.bigon.core.designsystem.components.CineSettingRow
import com.bigon.core.designsystem.components.CineShimmerCard
import com.bigon.core.designsystem.components.CineSnackbar
import com.bigon.core.designsystem.components.CineTonalButton
import com.bigon.core.designsystem.icons.CineIcons
import com.bigon.core.designsystem.theme.CineTheme
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * The living style guide: every token and every component on one canvas, in the
 * order of the design spec. Reviewing a theme change means opening this one
 * preview rather than hunting through screens.
 *
 * Rendered tall on purpose — scroll the preview, or open it in Interactive mode.
 */
@CineThemePreview
@Composable
private fun CineComponentGalleryPreview() {
    SinemaTheme {
        Column(
            modifier = Modifier
                .background(CineTheme.colors.background)
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
                CineMovieCard(title = "Midnight Reel", meta = "2026 · Thriller", rating = 8.4, onClick = {})
                CineMovieCard(title = "No rating variant", meta = "2023 · Documentary", rating = null, onClick = {})
                CineShimmerCard()
            }
            CineSectionHeader(title = "Trending today", onSeeAll = {})
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CineSampleData.cast.take(2).forEach { (name, role) ->
                    CineCastCard(name = name, role = role)
                }
            }
            CineListItem(
                title = "The Batman II",
                subtitle = "2026 · ★ 7.9 · Crime",
                selected = false,
                onClick = {},
            )
            CineListItem(
                title = "Dune: Part Three",
                subtitle = "2026 · ★ 8.4 · Sci-Fi",
                selected = true,
                onClick = {},
            )

            GalleryHeading("Level 2 · Input & control")
            CineSearchBar(query = "", onQueryChange = {})
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CineChip(label = "All", selected = true, onClick = {})
                CineChip(label = "Sci-Fi", selected = false, onClick = {})
                CineChip(label = "Drama", selected = false, onClick = {})
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CinePrimaryButton(text = "Watch trailer", leadingIcon = CineIcons.Play, onClick = {})
                CineTonalButton(text = "Retry", onClick = {})
                CineFavoriteToggle(checked = true, onCheckedChange = {})
            }
            CineSegmentedControl(
                options = listOf("System", "Dark", "Light"),
                selectedIndex = 1,
                onSelect = {},
                modifier = Modifier.fillMaxWidth(),
            )
            CineSettingRow(
                title = "Content language",
                subtitle = "Titles & overviews",
                value = "EN",
                icon = CineIcons.Movie,
                onClick = {},
                showDivider = false,
            )

            GalleryHeading("Level 2 · Feedback")
            CineOfflineBanner(message = "✈ You're offline — showing saved content")
            CineSnackbar(
                message = "Couldn't refresh — showing saved data",
                actionLabel = "RETRY",
                onAction = {},
            )
            CineLoadingIndicator(modifier = Modifier.padding(start = 8.dp))
            CineEmptyState(
                icon = CineIcons.HeartOutline,
                title = "No favorites yet",
                subtitle = "Movies you favorite appear here and work offline.",
                action = { CineTonalButton(text = "Browse trending", onClick = {}) },
                modifier = Modifier.fillMaxWidth(),
            )
            CineAttributionFooter(versionLabel = "Cinelog v1.0")
        }
    }
}

// ── gallery scaffolding ─────────────────────────────────────────────────────

@Composable
private fun GalleryHeading(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = text.uppercase(),
            style = CineTheme.typography.label,
            color = CineTheme.colors.textSecondary,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CineTheme.colors.outline),
        )
    }
}

@Composable
private fun ColorTokens() {
    val colors = CineTheme.colors
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

@Composable
private fun TypographyTokens() {
    val type = CineTheme.typography
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
            style = CineTheme.typography.caption,
            color = CineTheme.colors.textSecondary,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = "Dune: Part Three",
            style = style,
            color = CineTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun SpacingAndShapeTokens() {
    val spacing = CineTheme.spacing
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
            ShapeSample("card", CineTheme.shapes.card)
            ShapeSample("container", CineTheme.shapes.container)
            ShapeSample("pill", CineTheme.shapes.pill)
            ShapeSample("badge", CineTheme.shapes.badge)
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
                .background(CineTheme.colors.primaryContainer),
        )
        Text(
            text = "$name ${value.value.toInt()}",
            style = CineTheme.typography.caption,
            color = CineTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun ShapeSample(name: String, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(shape)
            .background(CineTheme.colors.surfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = CineTheme.typography.caption,
            color = CineTheme.colors.textSecondary,
        )
    }
}
