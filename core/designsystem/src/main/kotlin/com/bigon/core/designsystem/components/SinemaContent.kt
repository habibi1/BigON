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
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaRtlPreview
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * SinemaSectionHeader — with / without the "see all" affordance
 * (component gallery §Content).
 */
@Composable
fun SinemaSectionHeader(
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
            style = SinemaTheme.typography.title,
            color = SinemaTheme.colors.textPrimary,
        )
        if (onSeeAll != null) {
            Text(
                text = seeAllLabel,
                style = SinemaTheme.typography.label,
                color = SinemaTheme.colors.primary,
                modifier = Modifier
                    .clip(SinemaTheme.shapes.badge)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = SinemaTheme.spacing.xs, vertical = SinemaTheme.spacing.xs),
            )
        }
    }
}

/** SinemaCastCard — avatar + actor + role (component gallery §Content). */
@Composable
fun SinemaCastCard(
    name: String,
    role: String,
    modifier: Modifier = Modifier,
    avatar: @Composable BoxScope.() -> Unit = { SinemaAvatarPlaceholder() },
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(SinemaTheme.shapes.pill)
                .background(SinemaTheme.colors.surfaceHigh),
        ) {
            avatar()
        }
        Text(
            text = name,
            style = SinemaTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = SinemaTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = SinemaTheme.spacing.xs),
        )
        Text(
            text = role,
            style = SinemaTheme.typography.caption,
            color = SinemaTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.SinemaAvatarPlaceholder() {
    Icon(
        imageVector = SinemaIcons.Person,
        contentDescription = null,
        tint = SinemaTheme.colors.textSecondary,
        modifier = Modifier
            .size(28.dp)
            .align(Alignment.Center),
    )
}

/**
 * SinemaListItem — tablet list pane row, default / selected
 * (component gallery §Content).
 */
@Composable
fun SinemaListItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable BoxScope.() -> Unit = { SinemaPosterPlaceholder() },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.m),
        modifier = modifier
            .fillMaxWidth()
            .clip(SinemaTheme.shapes.card)
            .background(if (selected) SinemaTheme.colors.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = SinemaTheme.spacing.m, vertical = 10.dp),
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
                style = SinemaTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = SinemaTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = SinemaTheme.typography.caption,
                color = SinemaTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = SinemaTheme.spacing.xs / 2),
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun SinemaSectionHeaderPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSectionHeader(title = "🔥 Trending today", onSeeAll = {})
    }
}

/** No action slot — the header collapses to a plain title. */
@SinemaPreview
@Composable
private fun SinemaSectionHeaderTitleOnlyPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSectionHeader(title = "Cast")
    }
}

@SinemaPreview
@Composable
private fun SinemaSectionHeaderCustomActionPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSectionHeader(title = "Recommendations", onSeeAll = {}, seeAllLabel = "More")
    }
}

/** RTL: the action must mirror to the left edge. */
@SinemaRtlPreview
@Composable
private fun SinemaSectionHeaderRtlPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSectionHeader(title = "Trending today", onSeeAll = {})
    }
}

@SinemaThemePreview
@Composable
private fun SinemaCastCardPreview() {
    SinemaPreviewSurface {
        SinemaCastCard(name = "Zendaya", role = "Chani")
    }
}

/** Names and roles longer than the 72dp column must ellipsize, not wrap. */
@SinemaPreview
@Composable
private fun SinemaCastCardLongNamePreview() {
    SinemaPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SinemaCastCard(name = "T. Chalamet", role = "Paul Atreides")
            SinemaCastCard(name = "Rebecca Ferguson", role = "Lady Jessica Atreides")
        }
    }
}

@SinemaThemePreview
@Composable
private fun SinemaListItemPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaListItem(
            title = "The Batman II",
            subtitle = "2026 · ★ 7.9 · Crime",
            selected = false,
            onClick = {},
        )
    }
}

/** Selected state — the tablet list pane's current row. */
@SinemaPreview
@Composable
private fun SinemaListItemSelectedPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaListItem(
            title = "Dune: Part Three",
            subtitle = "2026 · ★ 8.4 · Sci-Fi",
            selected = true,
            onClick = {},
        )
    }
}

@SinemaPreview
@Composable
private fun SinemaListItemLongTextPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaListItem(
            title = "A Title Long Enough To Need Ellipsis Treatment",
            subtitle = "2026 · ★ 8.4 · Science Fiction · Adventure · Drama",
            selected = false,
            onClick = {},
        )
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaListItemFontScalePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaListItem(
            title = "Dune: Part Three",
            subtitle = "2026 · ★ 8.4 · Sci-Fi",
            selected = true,
            onClick = {},
        )
    }
}
