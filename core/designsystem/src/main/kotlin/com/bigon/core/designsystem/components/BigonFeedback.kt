package com.bigon.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.R
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * BigonOfflineBanner — N2.1 global connectivity banner
 * (component gallery §Scaffolding & feedback).
 */
@Composable
fun BigonOfflineBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(BigonTheme.shapes.badge)
            .background(BigonTheme.colors.errorContainer)
            .padding(BigonTheme.spacing.s),
    ) {
        Text(
            text = message,
            style = BigonTheme.typography.label,
            color = BigonTheme.colors.onErrorContainer,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * BigonSnackbar — transient notice with optional action, e.g. the F3.7
 * stale-cache "RETRY" (component gallery §Scaffolding & feedback).
 */
@Composable
fun BigonSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        shape = BigonTheme.shapes.card,
        color = BigonTheme.colors.surfaceHigh,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = message,
                style = BigonTheme.typography.body.copy(fontSize = 13.sp),
                color = BigonTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    style = BigonTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = BigonTheme.colors.primary,
                    modifier = Modifier
                        .clip(BigonTheme.shapes.badge)
                        .clickable(onClick = onAction)
                        .padding(BigonTheme.spacing.xs),
                )
            }
        }
    }
}

/** BigonLoadingIndicator — the single spinner used across the app. */
@Composable
fun BigonLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    CircularProgressIndicator(
        color = BigonTheme.colors.primary,
        trackColor = BigonTheme.colors.surfaceHigh,
        strokeWidth = 3.dp,
        modifier = modifier.size(size),
    )
}

/**
 * BigonEmptyState — slot-based icon + title + subtitle + optional CTA.
 * Also covers the error variant (404 / F3.7): same anatomy, different content
 * (component gallery §Scaffolding & feedback — "exists exactly once in code").
 */
@Composable
fun BigonEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            horizontal = BigonTheme.spacing.xl,
            vertical = BigonTheme.spacing.xl,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BigonTheme.colors.textSecondary,
            modifier = Modifier
                .size(40.dp)
                .alpha(0.5f),
        )
        Text(
            text = title,
            style = BigonTheme.typography.title.copy(fontSize = 15.sp),
            color = BigonTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = BigonTheme.spacing.m),
        )
        Text(
            text = subtitle,
            style = BigonTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = BigonTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = BigonTheme.spacing.s),
        )
        if (action != null) {
            Box(modifier = Modifier.padding(top = BigonTheme.spacing.m)) {
                action()
            }
        }
    }
}


// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonOfflineBannerPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonOfflineBanner(message = "✈ You're offline — showing saved content")
    }
}

/** A message long enough to wrap to a second line. */
@BigonPreview
@Composable
private fun BigonOfflineBannerLongMessagePreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonOfflineBanner(
            message = "You're offline — showing content saved on this device, which may be out of date",
        )
    }
}

@BigonThemePreview
@Composable
private fun BigonSnackbarPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSnackbar(
            message = "Couldn't refresh — showing saved data",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

/** No action slot — the message fills the width. */
@BigonPreview
@Composable
private fun BigonSnackbarNoActionPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSnackbar(message = "Added to favorites")
    }
}

@BigonPreview
@Composable
private fun BigonSnackbarLongMessagePreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSnackbar(
            message = "We couldn't reach TMDB, so you're seeing the copy saved on this device",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

@BigonThemePreview
@Composable
private fun BigonLoadingIndicatorPreview() {
    BigonPreviewSurface {
        BigonLoadingIndicator()
    }
}

/** The three sizes in use: inline, default, full-screen. */
@BigonPreview
@Composable
private fun BigonLoadingIndicatorSizesPreview() {
    BigonPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BigonLoadingIndicator(size = 16.dp)
            BigonLoadingIndicator()
            BigonLoadingIndicator(size = 48.dp)
        }
    }
}

@BigonThemePreview
@Composable
private fun BigonEmptyStatePreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonEmptyState(
            icon = BigonIcons.HeartOutline,
            title = "No favorites yet",
            subtitle = "Movies you favorite appear here and work offline.",
            action = { BigonTonalButton(text = "Browse trending", onClick = {}) },
        )
    }
}

/** Error variant — same component, no call to action. */
@BigonPreview
@Composable
private fun BigonEmptyStateErrorPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonEmptyState(
            icon = BigonIcons.Movie,
            title = "Movie unavailable",
            subtitle = "This title was removed from TMDB.",
        )
    }
}

/** No-results variant. */
@BigonPreview
@Composable
private fun BigonEmptyStateNoResultsPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonEmptyState(
            icon = BigonIcons.Search,
            title = "No results",
            subtitle = "Try a different title or genre.",
        )
    }
}

@BigonFontScalePreview
@Composable
private fun BigonEmptyStateFontScalePreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonEmptyState(
            icon = BigonIcons.HeartOutline,
            title = "No favorites yet",
            subtitle = "Movies you favorite appear here and work offline.",
            action = { BigonTonalButton(text = "Browse trending", onClick = {}) },
        )
    }
}

