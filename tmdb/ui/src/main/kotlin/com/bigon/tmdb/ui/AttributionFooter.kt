package com.bigon.tmdb.ui

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
import com.bigon.tmdb.ui.R
import com.bigon.core.designsystem.components.SinemaShimmerBox
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme


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

// ── Previews ───────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun SinemaAttributionFooterPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaAttributionFooter(versionLabel = "Sinema v0.0.1")
    }
}
