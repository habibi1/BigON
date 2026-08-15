package com.bigon.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonPreview
import com.bigon.core.designsystem.preview.BigonPreviewSurface
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * BigonSearchBar — pill search field, idle / focused with clear affordance
 * (component gallery §Input). Focus is signalled with a 2dp primary ring, the
 * clear affordance appears once there is input.
 */
@Composable
fun BigonSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search movies…",
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = BigonTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(BigonTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .then(
                if (focused) {
                    Modifier.border(2.dp, colors.primary, BigonTheme.shapes.pill)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp),
    ) {
        Icon(
            imageVector = BigonIcons.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = BigonTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = BigonTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = BigonIcons.Clear,
                contentDescription = "Clear search",
                tint = colors.textSecondary,
                modifier = Modifier
                    .clip(BigonTheme.shapes.pill)
                    .clickable { onQueryChange("") }
                    .padding(BigonTheme.spacing.xs)
                    .size(16.dp),
            )
        }
    }
}

/** BigonChip — filter chip, off / on (component gallery §Input). */
@Composable
fun BigonChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BigonTheme.colors
    Box(
        modifier = modifier
            .clip(BigonTheme.shapes.pill)
            .background(if (selected) colors.primaryContainer else colors.surfaceHigh)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.outline, BigonTheme.shapes.pill)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = BigonTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
        )
    }
}

/** BigonChipRow — horizontally scrolling single-choice chip row. */
@Composable
fun BigonChipRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BigonTheme.spacing.s),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            BigonChip(
                label = option,
                selected = option == selectedOption,
                onClick = { onOptionSelect(option) },
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun BigonSearchBarPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSearchBar(query = "", onQueryChange = {})
    }
}

/** With input: the clear affordance appears. */
@BigonPreview
@Composable
private fun BigonSearchBarFilledPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSearchBar(query = "interstellar", onQueryChange = {})
    }
}

@BigonPreview
@Composable
private fun BigonSearchBarCustomPlaceholderPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSearchBar(query = "", onQueryChange = {}, placeholder = "Search cast & crew…")
    }
}

@BigonPreview
@Composable
private fun BigonSearchBarDisabledPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSearchBar(query = "read only", onQueryChange = {}, enabled = false)
    }
}

/** Run this in Interactive Preview (▶) to see the focus ring and type. */
@BigonPreview
@Composable
private fun BigonSearchBarInteractivePreview() {
    var query by remember { mutableStateOf("") }
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSearchBar(query = query, onQueryChange = { query = it })
    }
}

@BigonFontScalePreview
@Composable
private fun BigonSearchBarFontScalePreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonSearchBar(query = "", onQueryChange = {})
    }
}

@BigonThemePreview
@Composable
private fun BigonChipPreview() {
    BigonPreviewSurface {
        BigonChip(label = "Sci-Fi", selected = false, onClick = {})
    }
}

@BigonPreview
@Composable
private fun BigonChipSelectedPreview() {
    BigonPreviewSurface {
        BigonChip(label = "Sci-Fi", selected = true, onClick = {})
    }
}

/** Both states side by side — the contrast between them is the design. */
@BigonPreview
@Composable
private fun BigonChipStatesPreview() {
    BigonPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigonChip(label = "All", selected = true, onClick = {})
            BigonChip(label = "Drama", selected = false, onClick = {})
        }
    }
}

@BigonThemePreview
@Composable
private fun BigonChipRowPreview() {
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonChipRow(
            options = listOf("All", "Sci-Fi", "Drama", "Romance", "Horror", "Comedy"),
            selectedOption = "All",
            onOptionSelect = {},
        )
    }
}

/** Interactive: tap chips to move the selection. */
@BigonPreview
@Composable
private fun BigonChipRowInteractivePreview() {
    var selected by remember { mutableStateOf("All") }
    BigonPreviewSurface(modifier = Modifier.width(320.dp)) {
        BigonChipRow(
            options = listOf("All", "Sci-Fi", "Drama", "Romance"),
            selectedOption = selected,
            onOptionSelect = { selected = it },
        )
    }
}
