package com.bigon.tmdb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.components.BigonShimmerBox
import com.bigon.core.designsystem.components.BigonShimmerText
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme


/** Shared with [BigonShimmerCastCard], so the two cannot drift apart. */
private val CastCardWidth = 72.dp
private val CastAvatarSize = 64.dp

/** BigonCastCard — avatar + actor + role (component gallery §Content). */
@Composable
fun BigonCastCard(
    name: String,
    role: String,
    modifier: Modifier = Modifier,
    avatar: @Composable BoxScope.() -> Unit = { BigonAvatarPlaceholder() },
) {
    Column(
        modifier = modifier.width(CastCardWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(CastAvatarSize)
                .clip(BigonTheme.shapes.pill)
                .background(BigonTheme.colors.surfaceHigh),
        ) {
            avatar()
        }
        Text(
            text = name,
            style = BigonTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = BigonTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = BigonTheme.spacing.xs),
        )
        Text(
            text = role,
            style = BigonTheme.typography.caption,
            color = BigonTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * BigonShimmerCastCard — the loading variant of [BigonCastCard].
 *
 * Same column width, same circular avatar, and two caption-sized lines where
 * the name and role will land, so a cast row does not change height when it
 * fills in. The role line is shorter than the name line because it is shorter
 * far more often than not.
 */
@Composable
fun BigonShimmerCastCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(CastCardWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BigonShimmerBox(
            shape = BigonTheme.shapes.pill,
            modifier = Modifier.size(CastAvatarSize),
        )
        BigonShimmerText(
            style = BigonTheme.typography.caption,
            modifier = Modifier
                .padding(top = BigonTheme.spacing.xs)
                .fillMaxWidth(),
        )
        BigonShimmerText(
            style = BigonTheme.typography.caption,
            modifier = Modifier.fillMaxWidth(0.7f),
        )
    }
}

// ── Previews ───────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonCastCardPreview() {
    BigonPreviewSurface {
        BigonCastCard(name = "Zendaya", role = "Chani")
    }
}

/** Names and roles longer than the 72dp column must ellipsize, not wrap. */
@BigonPreview
@Composable
private fun BigonCastCardLongNamePreview() {
    BigonPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigonCastCard(name = "T. Chalamet", role = "Paul Atreides")
            BigonCastCard(name = "Rebecca Ferguson", role = "Lady Jessica Atreides")
        }
    }
}

@Composable
private fun BoxScope.BigonAvatarPlaceholder() {
    Icon(
        imageVector = BigonIcons.Person,
        contentDescription = null,
        tint = BigonTheme.colors.textSecondary,
        modifier = Modifier
            .size(28.dp)
            .align(Alignment.Center),
    )
}
