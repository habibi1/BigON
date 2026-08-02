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
import com.bigon.core.designsystem.icons.CineIcons
import com.bigon.core.designsystem.preview.CineFontScalePreview
import com.bigon.core.designsystem.preview.CinePreview
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineThemePreview
import com.bigon.core.designsystem.theme.CineTheme

/**
 * CinePrimaryButton — high-emphasis pill action, min height 48dp (N3.4)
 * (component gallery §Input).
 */
@Composable
fun CinePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    CineButtonImpl(
        text = text,
        onClick = onClick,
        container = CineTheme.colors.primary,
        contentColor = CineTheme.colors.onPrimary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

/** CineTonalButton — medium-emphasis pill action (component gallery §Input). */
@Composable
fun CineTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    CineButtonImpl(
        text = text,
        onClick = onClick,
        container = CineTheme.colors.surfaceHigh,
        contentColor = CineTheme.colors.textPrimary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
    )
}

@Composable
private fun CineButtonImpl(
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
        horizontalArrangement = Arrangement.spacedBy(CineTheme.spacing.s),
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CineTheme.shapes.pill)
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
            style = CineTheme.typography.label.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = contentColor,
        )
    }
}

/** CineFavoriteToggle — 48dp heart toggle, off / on (component gallery §Input). */
@Composable
fun CineFavoriteToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CineTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CineTheme.shapes.pill)
            .background(if (checked) colors.primaryContainer else colors.surfaceHigh)
            .clickable { onCheckedChange(!checked) },
    ) {
        Icon(
            imageVector = if (checked) CineIcons.Heart else CineIcons.HeartOutline,
            contentDescription = if (checked) "Remove from favorites" else "Add to favorites",
            tint = if (checked) colors.favoriteActive else colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * CineSegmentedControl — single-choice segments, e.g. the Settings theme picker
 * (F5.1) (component gallery §Input).
 */
@Composable
fun CineSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CineTheme.colors
    Row(
        modifier = modifier
            .clip(CineTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .padding(CineTheme.spacing.xs),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(CineTheme.shapes.pill)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
            ) {
                Text(
                    text = option,
                    style = CineTheme.typography.label.copy(
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

@CineThemePreview
@Composable
private fun CinePrimaryButtonPreview() {
    CinePreviewSurface {
        CinePrimaryButton(text = "Watch trailer", leadingIcon = CineIcons.Play, onClick = {})
    }
}

/** Without the icon slot. */
@CinePreview
@Composable
private fun CinePrimaryButtonNoIconPreview() {
    CinePreviewSurface {
        CinePrimaryButton(text = "Retry", onClick = {})
    }
}

@CinePreview
@Composable
private fun CinePrimaryButtonDisabledPreview() {
    CinePreviewSurface {
        CinePrimaryButton(
            text = "Watch trailer",
            leadingIcon = CineIcons.Play,
            enabled = false,
            onClick = {},
        )
    }
}

@CinePreview
@Composable
private fun CinePrimaryButtonFullWidthPreview() {
    CinePreviewSurface(modifier = Modifier.width(300.dp)) {
        CinePrimaryButton(text = "Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@CineFontScalePreview
@Composable
private fun CinePrimaryButtonFontScalePreview() {
    CinePreviewSurface {
        CinePrimaryButton(text = "Watch trailer", leadingIcon = CineIcons.Play, onClick = {})
    }
}

@CineThemePreview
@Composable
private fun CineTonalButtonPreview() {
    CinePreviewSurface {
        CineTonalButton(text = "Browse trending", onClick = {})
    }
}

@CinePreview
@Composable
private fun CineTonalButtonWithIconPreview() {
    CinePreviewSurface {
        CineTonalButton(text = "Add to list", leadingIcon = CineIcons.HeartOutline, onClick = {})
    }
}

@CinePreview
@Composable
private fun CineTonalButtonDisabledPreview() {
    CinePreviewSurface {
        CineTonalButton(text = "Browse trending", enabled = false, onClick = {})
    }
}

/** Emphasis pair as a detail screen uses them. */
@CinePreview
@Composable
private fun CineButtonEmphasisPreview() {
    CinePreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CinePrimaryButton(text = "Watch trailer", leadingIcon = CineIcons.Play, onClick = {})
            CineTonalButton(text = "Retry", onClick = {})
        }
    }
}

@CineThemePreview
@Composable
private fun CineFavoriteTogglePreview() {
    CinePreviewSurface {
        CineFavoriteToggle(checked = false, onCheckedChange = {})
    }
}

@CinePreview
@Composable
private fun CineFavoriteToggleCheckedPreview() {
    CinePreviewSurface {
        CineFavoriteToggle(checked = true, onCheckedChange = {})
    }
}

/** Interactive: tap to toggle the heart. */
@CinePreview
@Composable
private fun CineFavoriteToggleInteractivePreview() {
    var favorite by remember { mutableStateOf(false) }
    CinePreviewSurface {
        CineFavoriteToggle(checked = favorite, onCheckedChange = { favorite = it })
    }
}

@CineThemePreview
@Composable
private fun CineSegmentedControlPreview() {
    CinePreviewSurface(modifier = Modifier.width(300.dp)) {
        CineSegmentedControl(
            options = listOf("System", "Dark", "Light"),
            selectedIndex = 1,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@CinePreview
@Composable
private fun CineSegmentedControlTwoOptionsPreview() {
    CinePreviewSurface(modifier = Modifier.width(300.dp)) {
        CineSegmentedControl(
            options = listOf("Movies", "TV"),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Four segments — the point where labels start to crowd. */
@CinePreview
@Composable
private fun CineSegmentedControlFourOptionsPreview() {
    var selected by remember { mutableIntStateOf(2) }
    CinePreviewSurface(modifier = Modifier.width(300.dp)) {
        CineSegmentedControl(
            options = listOf("Day", "Week", "Month", "Year"),
            selectedIndex = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
