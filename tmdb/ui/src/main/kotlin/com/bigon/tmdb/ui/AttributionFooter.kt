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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.tmdb.ui.R
import com.bigon.core.designsystem.components.BigonShimmerBox
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme


/**
 * TMDB API Terms of Use §3, verbatim, with the bracketed choice resolved to
 * "product". https://www.themoviedb.org/api-terms-of-use
 */
const val TMDB_ATTRIBUTION: String =
    "This product uses TMDB and the TMDB APIs but is not endorsed, certified, " +
        "or otherwise approved by TMDB."

/**
 * BigonAttributionFooter — TMDB's required notice: their mark plus the exact
 * wording they mandate, defined exactly once (N4.3) so no screen can paraphrase
 * it (component gallery §Scaffolding & feedback).
 *
 * [TMDB_ATTRIBUTION] is quoted character-for-character from §3 of TMDB's API
 * Terms of Use. It reads redundantly — "uses TMDB and the TMDB APIs" — because
 * the terms require crediting the database and the API separately, and it ends
 * on three verbs, not two. Both are load-bearing: this is a licence condition,
 * not copy. Do not tighten it.
 *
 * The mark is TMDB's official SVG, drawn in their gradient and never tinted.
 *
 * It used to be a black silhouette recoloured to the caption colour, because
 * black scored 1.14:1 against the dark background and WCAG asks 3:1 of a
 * graphical object. That reasoning had the rule backwards twice over: WCAG
 * 1.4.11 exempts logos from contrast requirements precisely because they are not
 * ours to adjust, and TMDB's terms are what actually govern this asset. The fix
 * for an invisible mark was the right file, not a colour filter.
 *
 * The gradient measures ~2:1 on the light background and ~8:1 on the dark one.
 * The low end is deliberate and correct: it is the mark as its owner draws it.
 */
@Composable
fun BigonAttributionFooter(
    versionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BigonTheme.spacing.m),
        modifier = modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tmdb_logo),
            contentDescription = "The Movie Database (TMDB)",
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = "$versionLabel · $TMDB_ATTRIBUTION",
            style = BigonTheme.typography.caption.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
            color = BigonTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Previews ───────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonAttributionFooterPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonAttributionFooter(versionLabel = "Sinema v0.0.1")
    }
}
