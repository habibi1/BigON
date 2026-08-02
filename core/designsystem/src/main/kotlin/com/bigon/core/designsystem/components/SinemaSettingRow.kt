package com.bigon.core.designsystem.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * SinemaSettingRow — leading icon bubble, title/subtitle, trailing value
 * (component gallery §Input). The trailing chevron appears when the row is
 * clickable.
 */
@Composable
fun SinemaSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val colors = SinemaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(SinemaTheme.shapes.card)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
        ) {
            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SinemaTheme.shapes.pill)
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
                    style = SinemaTheme.typography.body.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.textPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = SinemaTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = SinemaTheme.spacing.xs / 2),
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = SinemaTheme.typography.label,
                    color = colors.primary,
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = SinemaIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
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

@SinemaThemePreview
@Composable
private fun SinemaSettingRowPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(
            title = "Content language",
            subtitle = "Titles & overviews",
            value = "EN",
            icon = SinemaIcons.Movie,
            onClick = {},
        )
    }
}

/** No icon slot. */
@SinemaPreview
@Composable
private fun SinemaSettingRowNoIconPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(title = "Clear cache", subtitle = "favorites kept", value = "128 MB", onClick = {})
    }
}

/** Title only — no subtitle, no value. */
@SinemaPreview
@Composable
private fun SinemaSettingRowTitleOnlyPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(title = "About Sinema", icon = SinemaIcons.Movie, onClick = {})
    }
}

/** Not clickable: no chevron, no ripple. */
@SinemaPreview
@Composable
private fun SinemaSettingRowStaticPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(title = "App version", subtitle = "1.0 (1)", icon = SinemaIcons.Settings)
    }
}

/** Last row in a group — divider suppressed. */
@SinemaPreview
@Composable
private fun SinemaSettingRowNoDividerPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(
            title = "Clear cache",
            subtitle = "favorites kept",
            value = "128 MB",
            icon = SinemaIcons.Clear,
            onClick = {},
            showDivider = false,
        )
    }
}

/** A settings group, which is how the rows are actually seen. */
@SinemaPreview
@Composable
private fun SinemaSettingRowGroupPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(title = "Content language", subtitle = "Titles & overviews", value = "EN", icon = SinemaIcons.Movie, onClick = {})
        SinemaSettingRow(title = "Clear cache", subtitle = "favorites kept", value = "128 MB", icon = SinemaIcons.Clear, onClick = {})
        SinemaSettingRow(title = "About", icon = SinemaIcons.Settings, onClick = {}, showDivider = false)
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaSettingRowFontScalePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(340.dp)) {
        SinemaSettingRow(
            title = "Content language",
            subtitle = "Titles & overviews",
            value = "EN",
            icon = SinemaIcons.Movie,
            onClick = {},
            showDivider = false,
        )
    }
}
