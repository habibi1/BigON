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
import com.bigon.core.designsystem.icons.CineIcons
import com.bigon.core.designsystem.preview.CineFontScalePreview
import com.bigon.core.designsystem.preview.CinePreview
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineThemePreview
import com.bigon.core.designsystem.theme.CineTheme

/**
 * CineOfflineBanner — N2.1 global connectivity banner
 * (component gallery §Scaffolding & feedback).
 */
@Composable
fun CineOfflineBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(CineTheme.shapes.badge)
            .background(CineTheme.colors.errorContainer)
            .padding(CineTheme.spacing.s),
    ) {
        Text(
            text = message,
            style = CineTheme.typography.label,
            color = CineTheme.colors.onErrorContainer,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * CineSnackbar — transient notice with optional action, e.g. the F3.7
 * stale-cache "RETRY" (component gallery §Scaffolding & feedback).
 */
@Composable
fun CineSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        shape = CineTheme.shapes.card,
        color = CineTheme.colors.surfaceHigh,
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
                style = CineTheme.typography.body.copy(fontSize = 13.sp),
                color = CineTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    style = CineTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                    color = CineTheme.colors.primary,
                    modifier = Modifier
                        .clip(CineTheme.shapes.badge)
                        .clickable(onClick = onAction)
                        .padding(CineTheme.spacing.xs),
                )
            }
        }
    }
}

/** CineLoadingIndicator — the single spinner used across the app. */
@Composable
fun CineLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    CircularProgressIndicator(
        color = CineTheme.colors.primary,
        trackColor = CineTheme.colors.surfaceHigh,
        strokeWidth = 3.dp,
        modifier = modifier.size(size),
    )
}

/**
 * CineEmptyState — slot-based icon + title + subtitle + optional CTA.
 * Also covers the error variant (404 / F3.7): same anatomy, different content
 * (component gallery §Scaffolding & feedback — "exists exactly once in code").
 */
@Composable
fun CineEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            horizontal = CineTheme.spacing.xl,
            vertical = CineTheme.spacing.xl,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CineTheme.colors.textSecondary,
            modifier = Modifier
                .size(40.dp)
                .alpha(0.5f),
        )
        Text(
            text = title,
            style = CineTheme.typography.title.copy(fontSize = 15.sp),
            color = CineTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CineTheme.spacing.m),
        )
        Text(
            text = subtitle,
            style = CineTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = CineTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CineTheme.spacing.s),
        )
        if (action != null) {
            Box(modifier = Modifier.padding(top = CineTheme.spacing.m)) {
                action()
            }
        }
    }
}

/**
 * CineAttributionFooter — TMDB's required notice: their mark plus the exact
 * wording they mandate, defined exactly once (N4.3) so no screen can paraphrase
 * it (component gallery §Scaffolding & feedback).
 *
 * The bundled mark is a monochrome silhouette, so it is tinted to the caption
 * colour rather than drawn as-is. Untinted it is pure black, which scores 1.14:1
 * against the dark background — an attribution nobody can see is not an
 * attribution, so the tint is a compliance requirement, not styling.
 */
@Composable
fun CineAttributionFooter(
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CineTheme.spacing.m),
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tmdb_logo),
            contentDescription = "The Movie Database (TMDB)",
            colorFilter = ColorFilter.tint(CineTheme.colors.textSecondary),
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = "$versionLabel · This product uses the TMDB API but is not endorsed or certified by TMDB.",
            style = CineTheme.typography.caption.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
            color = CineTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@CineThemePreview
@Composable
private fun CineOfflineBannerPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineOfflineBanner(message = "✈ You're offline — showing saved content")
    }
}

/** A message long enough to wrap to a second line. */
@CinePreview
@Composable
private fun CineOfflineBannerLongMessagePreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineOfflineBanner(
            message = "You're offline — showing content saved on this device, which may be out of date",
        )
    }
}

@CineThemePreview
@Composable
private fun CineSnackbarPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSnackbar(
            message = "Couldn't refresh — showing saved data",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

/** No action slot — the message fills the width. */
@CinePreview
@Composable
private fun CineSnackbarNoActionPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSnackbar(message = "Added to favorites")
    }
}

@CinePreview
@Composable
private fun CineSnackbarLongMessagePreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSnackbar(
            message = "We couldn't reach TMDB, so you're seeing the copy saved on this device",
            actionLabel = "RETRY",
            onAction = {},
        )
    }
}

@CineThemePreview
@Composable
private fun CineLoadingIndicatorPreview() {
    CinePreviewSurface {
        CineLoadingIndicator()
    }
}

/** The three sizes in use: inline, default, full-screen. */
@CinePreview
@Composable
private fun CineLoadingIndicatorSizesPreview() {
    CinePreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CineLoadingIndicator(size = 16.dp)
            CineLoadingIndicator()
            CineLoadingIndicator(size = 48.dp)
        }
    }
}

@CineThemePreview
@Composable
private fun CineEmptyStatePreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineEmptyState(
            icon = CineIcons.HeartOutline,
            title = "No favorites yet",
            subtitle = "Movies you favorite appear here and work offline.",
            action = { CineTonalButton(text = "Browse trending", onClick = {}) },
        )
    }
}

/** Error variant — same component, no call to action. */
@CinePreview
@Composable
private fun CineEmptyStateErrorPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineEmptyState(
            icon = CineIcons.Movie,
            title = "Movie unavailable",
            subtitle = "This title was removed from TMDB.",
        )
    }
}

/** No-results variant. */
@CinePreview
@Composable
private fun CineEmptyStateNoResultsPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineEmptyState(
            icon = CineIcons.Search,
            title = "No results",
            subtitle = "Try a different title or genre.",
        )
    }
}

@CineFontScalePreview
@Composable
private fun CineEmptyStateFontScalePreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineEmptyState(
            icon = CineIcons.HeartOutline,
            title = "No favorites yet",
            subtitle = "Movies you favorite appear here and work offline.",
            action = { CineTonalButton(text = "Browse trending", onClick = {}) },
        )
    }
}

@CineThemePreview
@Composable
private fun CineAttributionFooterPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineAttributionFooter(versionLabel = "Cinelog v1.0")
    }
}
