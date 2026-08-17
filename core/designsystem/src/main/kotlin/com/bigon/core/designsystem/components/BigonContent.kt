package com.bigon.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonRtlPreview
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * BigonSectionHeader — with / without the "see all" affordance
 * (component gallery §Content).
 */
@Composable
fun BigonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    seeAllLabel: String = "See all",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = BigonTheme.typography.title,
            color = BigonTheme.colors.textPrimary,
        )
        if (onSeeAll != null) {
            Text(
                text = seeAllLabel,
                style = BigonTheme.typography.label,
                color = BigonTheme.colors.primary,
                modifier = Modifier
                    .focusRing(BigonTheme.shapes.badge, width = 2.dp)
                    .clip(BigonTheme.shapes.badge)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = BigonTheme.spacing.xs, vertical = BigonTheme.spacing.xs),
            )
        }
    }
}



/**
 * BigonListItem — tablet list pane row, default / selected
 * (component gallery §Content).
 */
@Composable
fun BigonListItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // No default: the placeholder used to be a film poster, which quietly made
    // this component about films. Callers say what a missing thumbnail looks
    // like in their domain.
    thumbnail: @Composable BoxScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BigonTheme.spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .focusRing(BigonTheme.shapes.card)
            .clip(BigonTheme.shapes.card)
            .background(if (selected) BigonTheme.colors.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = BigonTheme.spacing.m, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 78.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            thumbnail()
        }
        Column {
            Text(
                text = title,
                style = BigonTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = BigonTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = BigonTheme.typography.caption,
                color = BigonTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = BigonTheme.spacing.xs / 2),
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonSectionHeaderPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSectionHeader(title = "🔥 Trending today", onSeeAll = {})
    }
}

/** No action slot — the header collapses to a plain title. */
@BigonPreview
@Composable
private fun BigonSectionHeaderTitleOnlyPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSectionHeader(title = "Cast")
    }
}

@BigonPreview
@Composable
private fun BigonSectionHeaderCustomActionPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSectionHeader(title = "Recommendations", onSeeAll = {}, seeAllLabel = "More")
    }
}

/** RTL: the action must mirror to the left edge. */
@BigonRtlPreview
@Composable
private fun BigonSectionHeaderRtlPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSectionHeader(title = "Trending today", onSeeAll = {})
    }
}



@BigonThemePreview
@Composable
private fun BigonListItemPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonListItem(
            title = "The Batman II",
            subtitle = "2026 · ★ 7.9 · Crime",
            selected = false,
            onClick = {},
            thumbnail = { BigonShimmerBox(modifier = Modifier.matchParentSize()) },
        )
    }
}

/** Selected state — the tablet list pane's current row. */
@BigonPreview
@Composable
private fun BigonListItemSelectedPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonListItem(
            title = "Dune: Part Three",
            subtitle = "2026 · ★ 8.4 · Sci-Fi",
            selected = true,
            onClick = {},
            thumbnail = { BigonShimmerBox(modifier = Modifier.matchParentSize()) },
        )
    }
}

@BigonPreview
@Composable
private fun BigonListItemLongTextPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonListItem(
            title = "A Title Long Enough To Need Ellipsis Treatment",
            subtitle = "2026 · ★ 8.4 · Science Fiction · Adventure · Drama",
            selected = false,
            onClick = {},
            thumbnail = { BigonShimmerBox(modifier = Modifier.matchParentSize()) },
        )
    }
}

@BigonFontScalePreview
@Composable
private fun BigonListItemFontScalePreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonListItem(
            title = "Dune: Part Three",
            subtitle = "2026 · ★ 8.4 · Sci-Fi",
            selected = true,
            onClick = {},
            thumbnail = { BigonShimmerBox(modifier = Modifier.matchParentSize()) },
        )
    }
}
