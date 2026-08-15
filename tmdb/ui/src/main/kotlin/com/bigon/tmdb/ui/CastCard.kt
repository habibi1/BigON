package com.bigon.tmdb.ui

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
import com.bigon.core.designsystem.components.SinemaShimmerBox
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaRtlPreview
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme


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

// ── Previews ───────────────────────────────────────────────────────────────

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
