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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * BigonPrimaryButton — high-emphasis pill action, min height 48dp (N3.4)
 * (component gallery §Input).
 */
@Composable
fun BigonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    BigonButtonImpl(
        text = text,
        onClick = onClick,
        container = BigonTheme.colors.primary,
        contentColor = BigonTheme.colors.onPrimary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

/** BigonTonalButton — medium-emphasis pill action (component gallery §Input). */
@Composable
fun BigonTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    BigonButtonImpl(
        text = text,
        onClick = onClick,
        container = BigonTheme.colors.surfaceHigh,
        contentColor = BigonTheme.colors.textPrimary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

@Composable
private fun BigonButtonImpl(
    text: String,
    onClick: () -> Unit,
    container: Color,
    contentColor: Color,
    modifier: Modifier,
    enabled: Boolean,
    leadingIcon: ImageVector?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BigonTheme.spacing.s),
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .focusRing(BigonTheme.shapes.pill)
            .clip(BigonTheme.shapes.pill)
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            style = BigonTheme.typography.label.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = contentColor,
        )
    }
}

/** BigonFavoriteToggle — 48dp heart toggle, off / on (component gallery §Input). */
@Composable
fun BigonFavoriteToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BigonTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(BigonTheme.shapes.pill)
            .background(if (checked) colors.primaryContainer else colors.surfaceHigh)
            .clickable { onCheckedChange(!checked) },
    ) {
        Icon(
            imageVector = if (checked) BigonIcons.Heart else BigonIcons.HeartOutline,
            contentDescription = if (checked) "Remove from favorites" else "Add to favorites",
            tint = if (checked) colors.favoriteActive else colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * BigonSegmentedControl — single-choice segments, e.g. the Settings theme picker
 * (F5.1) (component gallery §Input).
 */
@Composable
fun BigonSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BigonTheme.colors
    Row(
        modifier = modifier
            .clip(BigonTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .padding(BigonTheme.spacing.xs),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(BigonTheme.shapes.pill)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
            ) {
                Text(
                    text = option,
                    style = BigonTheme.typography.label.copy(
                        fontSize = 12.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
                )
            }
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonPrimaryButtonPreview() {
    BigonPreviewSurface {
        BigonPrimaryButton(text = "Watch trailer", leadingIcon = BigonIcons.Play, onClick = {})
    }
}

/** Without the icon slot. */
@BigonPreview
@Composable
private fun BigonPrimaryButtonNoIconPreview() {
    BigonPreviewSurface {
        BigonPrimaryButton(text = "Retry", onClick = {})
    }
}

@BigonPreview
@Composable
private fun BigonPrimaryButtonDisabledPreview() {
    BigonPreviewSurface {
        BigonPrimaryButton(
            text = "Watch trailer",
            leadingIcon = BigonIcons.Play,
            enabled = false,
            onClick = {},
        )
    }
}

@BigonPreview
@Composable
private fun BigonPrimaryButtonFullWidthPreview() {
    BigonPreviewSurface(modifier = Modifier.width(300.dp)) {
        BigonPrimaryButton(text = "Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@BigonFontScalePreview
@Composable
private fun BigonPrimaryButtonFontScalePreview() {
    BigonPreviewSurface {
        BigonPrimaryButton(text = "Watch trailer", leadingIcon = BigonIcons.Play, onClick = {})
    }
}

@BigonThemePreview
@Composable
private fun BigonTonalButtonPreview() {
    BigonPreviewSurface {
        BigonTonalButton(text = "Browse trending", onClick = {})
    }
}

@BigonPreview
@Composable
private fun BigonTonalButtonWithIconPreview() {
    BigonPreviewSurface {
        BigonTonalButton(text = "Add to list", leadingIcon = BigonIcons.HeartOutline, onClick = {})
    }
}

@BigonPreview
@Composable
private fun BigonTonalButtonDisabledPreview() {
    BigonPreviewSurface {
        BigonTonalButton(text = "Browse trending", enabled = false, onClick = {})
    }
}

/** Emphasis pair as a detail screen uses them. */
@BigonPreview
@Composable
private fun BigonButtonEmphasisPreview() {
    BigonPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigonPrimaryButton(text = "Watch trailer", leadingIcon = BigonIcons.Play, onClick = {})
            BigonTonalButton(text = "Retry", onClick = {})
        }
    }
}

@BigonThemePreview
@Composable
private fun BigonFavoriteTogglePreview() {
    BigonPreviewSurface {
        BigonFavoriteToggle(checked = false, onCheckedChange = {})
    }
}

@BigonPreview
@Composable
private fun BigonFavoriteToggleCheckedPreview() {
    BigonPreviewSurface {
        BigonFavoriteToggle(checked = true, onCheckedChange = {})
    }
}

/** Interactive: tap to toggle the heart. */
@BigonPreview
@Composable
private fun BigonFavoriteToggleInteractivePreview() {
    var favorite by remember { mutableStateOf(false) }
    BigonPreviewSurface {
        BigonFavoriteToggle(checked = favorite, onCheckedChange = { favorite = it })
    }
}

@BigonThemePreview
@Composable
private fun BigonSegmentedControlPreview() {
    BigonPreviewSurface(modifier = Modifier.width(300.dp)) {
        BigonSegmentedControl(
            options = listOf("System", "Dark", "Light"),
            selectedIndex = 1,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@BigonPreview
@Composable
private fun BigonSegmentedControlTwoOptionsPreview() {
    BigonPreviewSurface(modifier = Modifier.width(300.dp)) {
        BigonSegmentedControl(
            options = listOf("Movies", "TV"),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Four segments — the point where labels start to crowd. */
@BigonPreview
@Composable
private fun BigonSegmentedControlFourOptionsPreview() {
    var selected by remember { mutableIntStateOf(2) }
    BigonPreviewSurface(modifier = Modifier.width(300.dp)) {
        BigonSegmentedControl(
            options = listOf("Day", "Week", "Month", "Year"),
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
