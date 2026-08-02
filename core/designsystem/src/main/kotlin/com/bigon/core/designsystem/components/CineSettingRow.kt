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
import com.bigon.core.designsystem.icons.CineIcons
import com.bigon.core.designsystem.preview.CineFontScalePreview
import com.bigon.core.designsystem.preview.CinePreview
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineThemePreview
import com.bigon.core.designsystem.theme.CineTheme

/**
 * CineSettingRow — leading icon bubble, title/subtitle, trailing value
 * (component gallery §Input). The trailing chevron appears when the row is
 * clickable.
 */
@Composable
fun CineSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val colors = CineTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CineTheme.shapes.card)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 14.dp),
        ) {
            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CineTheme.shapes.pill)
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
                    style = CineTheme.typography.body.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.textPrimary,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = CineTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = CineTheme.spacing.xs / 2),
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = CineTheme.typography.label,
                    color = colors.primary,
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = CineIcons.ChevronRight,
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

@CineThemePreview
@Composable
private fun CineSettingRowPreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(
            title = "Content language",
            subtitle = "Titles & overviews",
            value = "EN",
            icon = CineIcons.Movie,
            onClick = {},
        )
    }
}

/** No icon slot. */
@CinePreview
@Composable
private fun CineSettingRowNoIconPreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(title = "Clear cache", subtitle = "favorites kept", value = "128 MB", onClick = {})
    }
}

/** Title only — no subtitle, no value. */
@CinePreview
@Composable
private fun CineSettingRowTitleOnlyPreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(title = "About Cinelog", icon = CineIcons.Movie, onClick = {})
    }
}

/** Not clickable: no chevron, no ripple. */
@CinePreview
@Composable
private fun CineSettingRowStaticPreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(title = "App version", subtitle = "1.0 (1)", icon = CineIcons.Settings)
    }
}

/** Last row in a group — divider suppressed. */
@CinePreview
@Composable
private fun CineSettingRowNoDividerPreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(
            title = "Clear cache",
            subtitle = "favorites kept",
            value = "128 MB",
            icon = CineIcons.Clear,
            onClick = {},
            showDivider = false,
        )
    }
}

/** A settings group, which is how the rows are actually seen. */
@CinePreview
@Composable
private fun CineSettingRowGroupPreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(title = "Content language", subtitle = "Titles & overviews", value = "EN", icon = CineIcons.Movie, onClick = {})
        CineSettingRow(title = "Clear cache", subtitle = "favorites kept", value = "128 MB", icon = CineIcons.Clear, onClick = {})
        CineSettingRow(title = "About", icon = CineIcons.Settings, onClick = {}, showDivider = false)
    }
}

@CineFontScalePreview
@Composable
private fun CineSettingRowFontScalePreview() {
    CinePreviewSurface(modifier = Modifier.width(340.dp)) {
        CineSettingRow(
            title = "Content language",
            subtitle = "Titles & overviews",
            value = "EN",
            icon = CineIcons.Movie,
            onClick = {},
            showDivider = false,
        )
    }
}
