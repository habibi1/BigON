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
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * SinemaOfflineBanner — N2.1 global connectivity banner
 * (component gallery §Scaffolding & feedback).
 */
@Composable
fun SinemaOfflineBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(SinemaTheme.shapes.badge)
            .background(SinemaTheme.colors.errorContainer)
            .padding(SinemaTheme.spacing.s),
    ) {
        Text(
            text = message,
            style = SinemaTheme.typography.label,
            color = SinemaTheme.colors.onErrorContainer,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * SinemaSnackbar — transient notice with optional action, e.g. the F3.7
 * stale-cache "RETRY" (component gallery §Scaffolding & feedback).
 */
@Composable
fun SinemaSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        shape = SinemaTheme.shapes.card,
        color = SinemaTheme.colors.surfaceHigh,
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
                style = SinemaTheme.typography.body.copy(fontSize = 13.sp),
                color = SinemaTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    style = SinemaTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = SinemaTheme.colors.primary,
                    modifier = Modifier
                        .clip(SinemaTheme.shapes.badge)
                        .clickable(onClick = onAction)
                        .padding(SinemaTheme.spacing.xs),
                )
            }
        }
    }
}

/** SinemaLoadingIndicator — the single spinner used across the app. */
@Composable
fun SinemaLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    CircularProgressIndicator(
        color = SinemaTheme.colors.primary,
        trackColor = SinemaTheme.colors.surfaceHigh,
        strokeWidth = 3.dp,
        modifier = modifier.size(size),
    )
}

/**
 * SinemaEmptyState — slot-based icon + title + subtitle + optional CTA.
 * Also covers the error variant (404 / F3.7): same anatomy, different content
 * (component gallery §Scaffolding & feedback — "exists exactly once in code").
 */
@Composable
fun SinemaEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            horizontal = SinemaTheme.spacing.xl,
            vertical = SinemaTheme.spacing.xl,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SinemaTheme.colors.textSecondary,
            modifier = Modifier
                .size(40.dp)
                .alpha(0.5f),
        )
        Text(
            text = title,
            style = SinemaTheme.typography.title.copy(fontSize = 15.sp),
            color = SinemaTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = SinemaTheme.spacing.m),
        )
        Text(
            text = subtitle,
            style = SinemaTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = SinemaTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = SinemaTheme.spacing.s),
        )
        if (action != null) {
            Box(modifier = Modifier.padding(top = SinemaTheme.spacing.m)) {
                action()
            }
        }
    }
}

/**
 * SinemaAttributionFooter — TMDB's required notice: their mark plus the exact
 * wording they mandate, defined exactly once (N4.3) so no screen can paraphrase
 * it (component gallery §Scaffolding & feedback).
 *
 * The bundled mark is a monochrome silhouette, so it is tinted to the caption
 * colour rather than drawn as-is. Untinted it is pure black, which scores 1.14:1
 * against the dark background — an attribution nobody can see is not an
 * attribution, so the tint is a compliance requirement, not styling.
 */
@Composable
fun SinemaAttributionFooter(
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.m),
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tmdb_logo),
            contentDescription = "The Movie Database (TMDB)",
            colorFilter = ColorFilter.tint(SinemaTheme.colors.textSecondary),
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = "$versionLabel · This product uses the TMDB API but is not endorsed or certified by TMDB.",
            style = SinemaTheme.typography.caption.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
            color = SinemaTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun SinemaOfflineBannerPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaOfflineBanner(message = "✈ You're offline — showing saved content")
    }
}

/** A message long enough to wrap to a second line. */
@SinemaPreview
@Composable
private fun SinemaOfflineBannerLongMessagePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaOfflineBanner(
            message = "You're offline — showing content saved on this device, which may be out of date",
        )
    }
}

@SinemaThemePreview
@Composable
private fun SinemaSnackbarPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSnackbar(
            message = "Couldn't refresh — showing saved data",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

/** No action slot — the message fills the width. */
@SinemaPreview
@Composable
private fun SinemaSnackbarNoActionPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSnackbar(message = "Added to favorites")
    }
}

@SinemaPreview
@Composable
private fun SinemaSnackbarLongMessagePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSnackbar(
            message = "We couldn't reach TMDB, so you're seeing the copy saved on this device",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

@SinemaThemePreview
@Composable
private fun SinemaLoadingIndicatorPreview() {
    SinemaPreviewSurface {
        SinemaLoadingIndicator()
    }
}

/** The three sizes in use: inline, default, full-screen. */
@SinemaPreview
@Composable
private fun SinemaLoadingIndicatorSizesPreview() {
    SinemaPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SinemaLoadingIndicator(size = 16.dp)
            SinemaLoadingIndicator()
            SinemaLoadingIndicator(size = 48.dp)
        }
    }
}

@SinemaThemePreview
@Composable
private fun SinemaEmptyStatePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaEmptyState(
            icon = SinemaIcons.HeartOutline,
            title = "No favorites yet",
            subtitle = "Movies you favorite appear here and work offline.",
            action = { SinemaTonalButton(text = "Browse trending", onClick = {}) },
        )
    }
}

/** Error variant — same component, no call to action. */
@SinemaPreview
@Composable
private fun SinemaEmptyStateErrorPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaEmptyState(
            icon = SinemaIcons.Movie,
            title = "Movie unavailable",
            subtitle = "This title was removed from TMDB.",
        )
    }
}

/** No-results variant. */
@SinemaPreview
@Composable
private fun SinemaEmptyStateNoResultsPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaEmptyState(
            icon = SinemaIcons.Search,
            title = "No results",
            subtitle = "Try a different title or genre.",
        )
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaEmptyStateFontScalePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaEmptyState(
            icon = SinemaIcons.HeartOutline,
            title = "No favorites yet",
            subtitle = "Movies you favorite appear here and work offline.",
            action = { SinemaTonalButton(text = "Browse trending", onClick = {}) },
        )
    }
}

@SinemaThemePreview
@Composable
private fun SinemaAttributionFooterPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaAttributionFooter(versionLabel = "Sinema v0.0.1")
    }
}
