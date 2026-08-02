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
import com.bigon.core.designsystem.icons.CineIcons
import com.bigon.core.designsystem.preview.CineFontScalePreview
import com.bigon.core.designsystem.preview.CinePreview
import com.bigon.core.designsystem.preview.CinePreviewSurface
import com.bigon.core.designsystem.preview.CineThemePreview
import com.bigon.core.designsystem.theme.CineTheme

/**
 * CineSearchBar — pill search field, idle / focused with clear affordance
 * (component gallery §Input). Focus is signalled with a 2dp primary ring, the
 * clear affordance appears once there is input.
 */
@Composable
fun CineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search movies…",
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = CineTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CineTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .then(
                if (focused) {
                    Modifier.border(2.dp, colors.primary, CineTheme.shapes.pill)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp),
    ) {
        Icon(
            imageVector = CineIcons.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = CineTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = CineTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = CineIcons.Clear,
                contentDescription = "Clear search",
                tint = colors.textSecondary,
                modifier = Modifier
                    .clip(CineTheme.shapes.pill)
                    .clickable { onQueryChange("") }
                    .padding(CineTheme.spacing.xs)
                    .size(16.dp),
            )
        }
    }
}

/** CineChip — filter chip, off / on (component gallery §Input). */
@Composable
fun CineChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CineTheme.colors
    Box(
        modifier = modifier
            .clip(CineTheme.shapes.pill)
            .background(if (selected) colors.primaryContainer else colors.surfaceHigh)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.outline, CineTheme.shapes.pill)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = CineTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
        )
    }
}

/** CineChipRow — horizontally scrolling single-choice chip row. */
@Composable
fun CineChipRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CineTheme.spacing.s),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            CineChip(
                label = option,
                selected = option == selectedOption,
                onClick = { onOptionSelect(option) },
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@CineThemePreview
@Composable
private fun CineSearchBarPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSearchBar(query = "", onQueryChange = {})
    }
}

/** With input: the clear affordance appears. */
@CinePreview
@Composable
private fun CineSearchBarFilledPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSearchBar(query = "interstellar", onQueryChange = {})
    }
}

@CinePreview
@Composable
private fun CineSearchBarCustomPlaceholderPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSearchBar(query = "", onQueryChange = {}, placeholder = "Search cast & crew…")
    }
}

@CinePreview
@Composable
private fun CineSearchBarDisabledPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSearchBar(query = "read only", onQueryChange = {}, enabled = false)
    }
}

/** Run this in Interactive Preview (▶) to see the focus ring and type. */
@CinePreview
@Composable
private fun CineSearchBarInteractivePreview() {
    var query by remember { mutableStateOf("") }
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSearchBar(query = query, onQueryChange = { query = it })
    }
}

@CineFontScalePreview
@Composable
private fun CineSearchBarFontScalePreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineSearchBar(query = "", onQueryChange = {})
    }
}

@CineThemePreview
@Composable
private fun CineChipPreview() {
    CinePreviewSurface {
        CineChip(label = "Sci-Fi", selected = false, onClick = {})
    }
}

@CinePreview
@Composable
private fun CineChipSelectedPreview() {
    CinePreviewSurface {
        CineChip(label = "Sci-Fi", selected = true, onClick = {})
    }
}

/** Both states side by side — the contrast between them is the design. */
@CinePreview
@Composable
private fun CineChipStatesPreview() {
    CinePreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CineChip(label = "All", selected = true, onClick = {})
            CineChip(label = "Drama", selected = false, onClick = {})
        }
    }
}

@CineThemePreview
@Composable
private fun CineChipRowPreview() {
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineChipRow(
            options = listOf("All", "Sci-Fi", "Drama", "Romance", "Horror", "Comedy"),
            selectedOption = "All",
            onOptionSelect = {},
        )
    }
}

/** Interactive: tap chips to move the selection. */
@CinePreview
@Composable
private fun CineChipRowInteractivePreview() {
    var selected by remember { mutableStateOf("All") }
    CinePreviewSurface(modifier = Modifier.width(320.dp)) {
        CineChipRow(
            options = listOf("All", "Sci-Fi", "Drama", "Romance"),
            selectedOption = selected,
            onOptionSelect = { selected = it },
        )
    }
}
