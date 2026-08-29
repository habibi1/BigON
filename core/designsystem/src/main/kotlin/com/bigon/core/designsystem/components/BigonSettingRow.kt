package com.bigon.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/** Share of the row a trailing value may occupy before it has to wrap. */
private const val VALUE_MAX_WIDTH_FRACTION = 0.42f

/**
 * BigonSettingRow — leading icon bubble, title/subtitle, trailing value
 * (component gallery §Input). The trailing chevron appears when the row is
 * clickable.
 */
@Composable
fun BigonSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val colors = BigonTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints {
            val valueMaxWidth = maxWidth * VALUE_MAX_WIDTH_FRACTION
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BigonTheme.shapes.card)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(vertical = 14.dp),
            ) {
                if (icon != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(BigonTheme.shapes.pill)
                            .background(colors.surfaceVariant),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = BigonTheme.typography.body.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = colors.textPrimary,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = BigonTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = BigonTheme.spacing.xs / 2),
                        )
                    }
                }
                if (value != null) {
                    // Capped, because an unbounded trailing value wins the whole
                    // measure: Row sizes children without a weight first and hands
                    // the weighted title column whatever is left, so one long value
                    // — "United States of America · device" — shreds the subtitle
                    // beside it into four one-word lines. End-aligned so values
                    // stack in a column down the screen whatever their length.
                    Text(
                        text = value,
                        style = BigonTheme.typography.label,
                        color = colors.primary,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = valueMaxWidth),
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = BigonIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                } else if (value != null) {
                    // Holds the chevron's place on a row that has none, so a value
                    // without one does not sit hard against the edge while every
                    // value above it is inset.
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.surfaceVariant),
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonSettingRowPreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(
            title = "Content language",
            subtitle = "Titles & overviews",
            value = "EN",
            icon = BigonIcons.Movie,
            onClick = {},
        )
    }
}

/** No icon slot. */
@BigonPreview
@Composable
private fun BigonSettingRowNoIconPreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(title = "Clear cache", subtitle = "favorites kept", value = "128 MB", onClick = {})
    }
}

/** Title only — no subtitle, no value. */
@BigonPreview
@Composable
private fun BigonSettingRowTitleOnlyPreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(title = "About Sinema", icon = BigonIcons.Movie, onClick = {})
    }
}

/** Not clickable: no chevron, no ripple. */
@BigonPreview
@Composable
private fun BigonSettingRowStaticPreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(title = "App version", subtitle = "1.0 (1)", icon = BigonIcons.Settings)
    }
}

/** Last row in a group — divider suppressed. */
@BigonPreview
@Composable
private fun BigonSettingRowNoDividerPreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(
            title = "Clear cache",
            subtitle = "favorites kept",
            value = "128 MB",
            icon = BigonIcons.Clear,
            onClick = {},
            showDivider = false,
        )
    }
}

/** A settings group, which is how the rows are actually seen. */
@BigonPreview
@Composable
private fun BigonSettingRowGroupPreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(title = "Content language", subtitle = "Titles & overviews", value = "EN", icon = BigonIcons.Movie, onClick = {})
        BigonSettingRow(title = "Clear cache", subtitle = "favorites kept", value = "128 MB", icon = BigonIcons.Clear, onClick = {})
        BigonSettingRow(title = "About", icon = BigonIcons.Settings, onClick = {}, showDivider = false)
    }
}

@BigonFontScalePreview
@Composable
private fun BigonSettingRowFontScalePreview() {
    BigonPreviewSurface(modifier = Modifier.width(340.dp)) {
        BigonSettingRow(
            title = "Content language",
            subtitle = "Titles & overviews",
            value = "EN",
            icon = BigonIcons.Movie,
            onClick = {},
            showDivider = false,
        )
    }
}
