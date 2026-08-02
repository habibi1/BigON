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
import com.bigon.core.designsystem.icons.CineIcons
import com.bigon.core.designsystem.preview.CineFontScalePreview
import com.bigon.core.designsystem.preview.CinePreview
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineRtlPreview
import com.bigon.core.designsystem.preview.CineThemePreview
import com.bigon.core.designsystem.theme.CineTheme

/**
 * CineSectionHeader — with / without the "see all" affordance
 * (component gallery §Content).
 */
@Composable
fun CineSectionHeader(
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
            style = CineTheme.typography.title,
            color = CineTheme.colors.textPrimary,
        )
        if (onSeeAll != null) {
            Text(
                text = seeAllLabel,
                style = CineTheme.typography.label,
                color = CineTheme.colors.primary,
                modifier = Modifier
                    .clip(CineTheme.shapes.badge)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = CineTheme.spacing.xs, vertical = CineTheme.spacing.xs),
            )
        }
    }
}

/** CineCastCard — avatar + actor + role (component gallery §Content). */
@Composable
fun CineCastCard(
    name: String,
    role: String,
    modifier: Modifier = Modifier,
    avatar: @Composable BoxScope.() -> Unit = { CineAvatarPlaceholder() },
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CineTheme.shapes.pill)
                .background(CineTheme.colors.surfaceHigh),
        ) {
            avatar()
        }
        Text(
            text = name,
            style = CineTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = CineTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CineTheme.spacing.xs),
        )
        Text(
            text = role,
            style = CineTheme.typography.caption,
            color = CineTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.CineAvatarPlaceholder() {
    Icon(
        imageVector = CineIcons.Person,
        contentDescription = null,
        tint = CineTheme.colors.textSecondary,
        modifier = Modifier
            .size(28.dp)
            .align(Alignment.Center),
    )
}

/**
 * CineListItem — tablet list pane row, default / selected
 * (component gallery §Content).
 */
@Composable
fun CineListItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable BoxScope.() -> Unit = { CinePosterPlaceholder() },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CineTheme.spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .clip(CineTheme.shapes.card)
            .background(if (selected) CineTheme.colors.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = CineTheme.spacing.m, vertical = 10.dp),
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
                style = CineTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = CineTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = CineTheme.typography.caption,
                color = CineTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = CineTheme.spacing.xs / 2),
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@CineThemePreview
@Composable
private fun CineSectionHeaderPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSectionHeader(title = "🔥 Trending today", onSeeAll = {})
    }
}

/** No action slot — the header collapses to a plain title. */
@CinePreview
@Composable
private fun CineSectionHeaderTitleOnlyPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSectionHeader(title = "Cast")
    }
}

@CinePreview
@Composable
private fun CineSectionHeaderCustomActionPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSectionHeader(title = "Recommendations", onSeeAll = {}, seeAllLabel = "More")
    }
}

/** RTL: the action must mirror to the left edge. */
@CineRtlPreview
@Composable
private fun CineSectionHeaderRtlPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSectionHeader(title = "Trending today", onSeeAll = {})
    }
}

@CineThemePreview
@Composable
private fun CineCastCardPreview() {
    CinePreviewSurface {
        CineCastCard(name = "Zendaya", role = "Chani")
    }
}

/** Names and roles longer than the 72dp column must ellipsize, not wrap. */
@CinePreview
@Composable
private fun CineCastCardLongNamePreview() {
    CinePreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CineCastCard(name = "T. Chalamet", role = "Paul Atreides")
            CineCastCard(name = "Rebecca Ferguson", role = "Lady Jessica Atreides")
        }
    }
}

@CineThemePreview
@Composable
private fun CineListItemPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineListItem(
            title = "The Batman II",
            subtitle = "2026 · ★ 7.9 · Crime",
            selected = false,
            onClick = {},
        )
    }
}

/** Selected state — the tablet list pane's current row. */
@CinePreview
@Composable
private fun CineListItemSelectedPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineListItem(
            title = "Dune: Part Three",
            subtitle = "2026 · ★ 8.4 · Sci-Fi",
            selected = true,
            onClick = {},
        )
    }
}

@CinePreview
@Composable
private fun CineListItemLongTextPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineListItem(
            title = "A Title Long Enough To Need Ellipsis Treatment",
            subtitle = "2026 · ★ 8.4 · Science Fiction · Adventure · Drama",
            selected = false,
            onClick = {},
        )
    }
}

@CineFontScalePreview
@Composable
private fun CineListItemFontScalePreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineListItem(
            title = "Dune: Part Three",
            subtitle = "2026 · ★ 8.4 · Sci-Fi",
            selected = true,
            onClick = {},
        )
    }
}
