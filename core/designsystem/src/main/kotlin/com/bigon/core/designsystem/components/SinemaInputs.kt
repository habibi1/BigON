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
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaPreview
import com.bigon.core.designsystem.preview.SinemaPreviewSurface
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme

/**
 * SinemaSearchBar — pill search field, idle / focused with clear affordance
 * (component gallery §Input). Focus is signalled with a 2dp primary ring, the
 * clear affordance appears once there is input.
 */
@Composable
fun SinemaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search movies…",
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = SinemaTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(SinemaTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .then(
                if (focused) {
                    Modifier.border(2.dp, colors.primary, SinemaTheme.shapes.pill)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp),
    ) {
        Icon(
            imageVector = SinemaIcons.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = SinemaTheme.typography.body,
                    color = colors.textSecondary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = SinemaTheme.typography.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = SinemaIcons.Clear,
                contentDescription = "Clear search",
                tint = colors.textSecondary,
                modifier = Modifier
                    .clip(SinemaTheme.shapes.pill)
                    .clickable { onQueryChange("") }
                    .padding(SinemaTheme.spacing.xs)
                    .size(16.dp),
            )
        }
    }
}

/** SinemaChip — filter chip, off / on (component gallery §Input). */
@Composable
fun SinemaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SinemaTheme.colors
    Box(
        modifier = modifier
            .clip(SinemaTheme.shapes.pill)
            .background(if (selected) colors.primaryContainer else colors.surfaceHigh)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.outline, SinemaTheme.shapes.pill)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = SinemaTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
        )
    }
}

/** SinemaChipRow — horizontally scrolling single-choice chip row. */
@Composable
fun SinemaChipRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.s),
        modifier = modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            SinemaChip(
                label = option,
                selected = option == selectedOption,
                onClick = { onOptionSelect(option) },
            )
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun SinemaSearchBarPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSearchBar(query = "", onQueryChange = {})
    }
}

/** With input: the clear affordance appears. */
@SinemaPreview
@Composable
private fun SinemaSearchBarFilledPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSearchBar(query = "interstellar", onQueryChange = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaSearchBarCustomPlaceholderPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSearchBar(query = "", onQueryChange = {}, placeholder = "Search cast & crew…")
    }
}

@SinemaPreview
@Composable
private fun SinemaSearchBarDisabledPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSearchBar(query = "read only", onQueryChange = {}, enabled = false)
    }
}

/** Run this in Interactive Preview (▶) to see the focus ring and type. */
@SinemaPreview
@Composable
private fun SinemaSearchBarInteractivePreview() {
    var query by remember { mutableStateOf("") }
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSearchBar(query = query, onQueryChange = { query = it })
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaSearchBarFontScalePreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaSearchBar(query = "", onQueryChange = {})
    }
}

@SinemaThemePreview
@Composable
private fun SinemaChipPreview() {
    SinemaPreviewSurface {
        SinemaChip(label = "Sci-Fi", selected = false, onClick = {})
    }
}

@SinemaPreview
@Composable
private fun SinemaChipSelectedPreview() {
    SinemaPreviewSurface {
        SinemaChip(label = "Sci-Fi", selected = true, onClick = {})
    }
}

/** Both states side by side — the contrast between them is the design. */
@SinemaPreview
@Composable
private fun SinemaChipStatesPreview() {
    SinemaPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SinemaChip(label = "All", selected = true, onClick = {})
            SinemaChip(label = "Drama", selected = false, onClick = {})
        }
    }
}

@SinemaThemePreview
@Composable
private fun SinemaChipRowPreview() {
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaChipRow(
            options = listOf("All", "Sci-Fi", "Drama", "Romance", "Horror", "Comedy"),
            selectedOption = "All",
            onOptionSelect = {},
        )
    }
}

/** Interactive: tap chips to move the selection. */
@SinemaPreview
@Composable
private fun SinemaChipRowInteractivePreview() {
    var selected by remember { mutableStateOf("All") }
    SinemaPreviewSurface(modifier = Modifier.width(320.dp)) {
        SinemaChipRow(
            options = listOf("All", "Sci-Fi", "Drama", "Romance"),
            selectedOption = selected,
            onOptionSelect = { selected = it },
        )
    }
}
