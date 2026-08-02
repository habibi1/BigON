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
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * SinemaPrimaryButton — high-emphasis pill action, min height 48dp (N3.4)
 * (component gallery §Input).
 */
@Composable
fun SinemaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    SinemaButtonImpl(
        text = text,
        onClick = onClick,
        container = SinemaTheme.colors.primary,
        contentColor = SinemaTheme.colors.onPrimary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

/** SinemaTonalButton — medium-emphasis pill action (component gallery §Input). */
@Composable
fun SinemaTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    SinemaButtonImpl(
        text = text,
        onClick = onClick,
        container = SinemaTheme.colors.surfaceHigh,
        contentColor = SinemaTheme.colors.textPrimary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

@Composable
private fun SinemaButtonImpl(
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
        horizontalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.s),
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(SinemaTheme.shapes.pill)
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
            style = SinemaTheme.typography.label.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = contentColor,
        )
    }
}

/** SinemaFavoriteToggle — 48dp heart toggle, off / on (component gallery §Input). */
@Composable
fun SinemaFavoriteToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SinemaTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(SinemaTheme.shapes.pill)
            .background(if (checked) colors.primaryContainer else colors.surfaceHigh)
            .clickable { onCheckedChange(!checked) },
    ) {
        Icon(
            imageVector = if (checked) SinemaIcons.Heart else SinemaIcons.HeartOutline,
            contentDescription = if (checked) "Remove from favorites" else "Add to favorites",
            tint = if (checked) colors.favoriteActive else colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * SinemaSegmentedControl — single-choice segments, e.g. the Settings theme picker
 * (F5.1) (component gallery §Input).
 */
@Composable
fun SinemaSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SinemaTheme.colors
    Row(
        modifier = modifier
            .clip(SinemaTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .padding(SinemaTheme.spacing.xs),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(SinemaTheme.shapes.pill)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
            ) {
                Text(
                    text = option,
                    style = SinemaTheme.typography.label.copy(
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

@SinemaThemePreview
@Composable
private fun SinemaPrimaryButtonPreview() {
    SinemaPreviewSurface {
        SinemaPrimaryButton(text = "Watch trailer", leadingIcon = SinemaIcons.Play, onClick = {})
    }
}

/** Without the icon slot. */
@SinemaPreview
@Composable
private fun SinemaPrimaryButtonNoIconPreview() {
    SinemaPreviewSurface {
        SinemaPrimaryButton(text = "Retry", onClick = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaPrimaryButtonDisabledPreview() {
    SinemaPreviewSurface {
        SinemaPrimaryButton(
            text = "Watch trailer",
            leadingIcon = SinemaIcons.Play,
            enabled = false,
            onClick = {},
        )
    }
}

@SinemaPreview
@Composable
private fun SinemaPrimaryButtonFullWidthPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(300.dp)) {
        SinemaPrimaryButton(text = "Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaPrimaryButtonFontScalePreview() {
    SinemaPreviewSurface {
        SinemaPrimaryButton(text = "Watch trailer", leadingIcon = SinemaIcons.Play, onClick = {})
    }
}

@SinemaThemePreview
@Composable
private fun SinemaTonalButtonPreview() {
    SinemaPreviewSurface {
        SinemaTonalButton(text = "Browse trending", onClick = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaTonalButtonWithIconPreview() {
    SinemaPreviewSurface {
        SinemaTonalButton(text = "Add to list", leadingIcon = SinemaIcons.HeartOutline, onClick = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaTonalButtonDisabledPreview() {
    SinemaPreviewSurface {
        SinemaTonalButton(text = "Browse trending", enabled = false, onClick = {})
    }
}

/** Emphasis pair as a detail screen uses them. */
@SinemaPreview
@Composable
private fun SinemaButtonEmphasisPreview() {
    SinemaPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SinemaPrimaryButton(text = "Watch trailer", leadingIcon = SinemaIcons.Play, onClick = {})
            SinemaTonalButton(text = "Retry", onClick = {})
        }
    }
}

@SinemaThemePreview
@Composable
private fun SinemaFavoriteTogglePreview() {
    SinemaPreviewSurface {
        SinemaFavoriteToggle(checked = false, onCheckedChange = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaFavoriteToggleCheckedPreview() {
    SinemaPreviewSurface {
        SinemaFavoriteToggle(checked = true, onCheckedChange = {})
    }
}

/** Interactive: tap to toggle the heart. */
@SinemaPreview
@Composable
private fun SinemaFavoriteToggleInteractivePreview() {
    var favorite by remember { mutableStateOf(false) }
    SinemaPreviewSurface {
        SinemaFavoriteToggle(checked = favorite, onCheckedChange = { favorite = it })
    }
}

@SinemaThemePreview
@Composable
private fun SinemaSegmentedControlPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(300.dp)) {
        SinemaSegmentedControl(
            options = listOf("System", "Dark", "Light"),
            selectedIndex = 1,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@SinemaPreview
@Composable
private fun SinemaSegmentedControlTwoOptionsPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(300.dp)) {
        SinemaSegmentedControl(
            options = listOf("Movies", "TV"),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Four segments — the point where labels start to crowd. */
@SinemaPreview
@Composable
private fun SinemaSegmentedControlFourOptionsPreview() {
    var selected by remember { mutableIntStateOf(2) }
    SinemaPreviewSurface(modifier = Modifier.width(300.dp)) {
        SinemaSegmentedControl(
            options = listOf("Day", "Week", "Month", "Year"),
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
