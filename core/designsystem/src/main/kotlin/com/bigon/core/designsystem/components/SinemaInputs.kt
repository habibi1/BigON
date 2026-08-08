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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.sp
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

/**
 * SinemaChip — compact metadata chip, off / on (component gallery §Input).
 *
 * For labelling something already on screen: a certification, a genre, a
 * keyword. It stays at its intrinsic ~28dp because a chip nobody taps should
 * not reserve a tap area. Anything a user actually operates wants
 * [SinemaFilterChip] instead, which is the same pill grown to a real target.
 */
@Composable
fun SinemaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipPill(label = label, selected = selected, modifier = modifier, onClick = onClick)
}

/**
 * SinemaFilterChip — interactive filter chip (component gallery §Input).
 *
 * The same pill as [SinemaChip] so a filter row and a metadata row read as one
 * vocabulary, with three differences that only matter once a finger is
 * involved:
 *
 *  - the pill is 40dp tall and sits in a 48dp touch target, meeting the
 *    minimum rather than the 28dp a text-sized chip lands on;
 *  - [count] renders a badge, so "how many refinements are on" is answerable
 *    without opening anything;
 *  - [leading] is a slot rather than an `ImageVector`, because the streaming
 *    filter puts a provider logo there and a logo is an image, not an icon —
 *    and the design system deliberately has no image loader.
 */
@Composable
fun SinemaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    leading: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .clip(SinemaTheme.shapes.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ChipPill(
            label = label,
            selected = selected,
            modifier = Modifier.heightIn(min = FILTER_CHIP_HEIGHT),
            count = count,
            leading = leading,
        )
    }
}

/**
 * The pill both chips are made of. The click lands here for [SinemaChip] and
 * on the padded box outside it for [SinemaFilterChip], which is the whole
 * difference between a label and a control.
 */
@Composable
private fun ChipPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    count: Int? = null,
    leading: @Composable (() -> Unit)? = null,
) {
    val colors = SinemaTheme.colors
    val shape = SinemaTheme.shapes.pill
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SinemaTheme.spacing.s),
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceHigh)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, colors.outline, shape)
                },
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        leading?.invoke()
        Text(
            text = label,
            style = SinemaTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) colors.onPrimaryContainer else colors.textSecondary,
        )
        if (count != null && count > 0) {
            ChipCountBadge(count = count, selected = selected)
        }
    }
}

/**
 * The count badge inverts against its chip rather than picking a fixed colour,
 * so it stays legible on both the selected and unselected pill.
 */
@Composable
private fun ChipCountBadge(count: Int, selected: Boolean) {
    val colors = SinemaTheme.colors
    Box(
        // A minimum rather than a fixed size: the digit grows with the font
        // scale, and a badge that clips its own number is worse than a wide one.
        modifier = Modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .clip(SinemaTheme.shapes.pill)
            .background(if (selected) colors.onPrimaryContainer else colors.primary)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = SinemaTheme.typography.caption.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = if (selected) colors.primaryContainer else colors.onPrimary,
        )
    }
}

/**
 * SinemaChipRow — horizontally scrolling single-choice chip row.
 *
 * Built from [SinemaFilterChip], not [SinemaChip]: every row of this kind is
 * something the user picks from, so the row is 48dp tall by construction.
 */
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
            SinemaFilterChip(
                label = option,
                selected = option == selectedOption,
                onClick = { onOptionSelect(option) },
            )
        }
    }
}

/** Android's minimum interactive size — the reason a filter chip is not a label. */
private val MIN_TOUCH_TARGET = 48.dp

/** Visual pill height for an operable chip; the rest of the 48dp is tap area. */
private val FILTER_CHIP_HEIGHT = 40.dp

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
private fun SinemaFilterChipPreview() {
    SinemaPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SinemaFilterChip(label = "All", selected = true, onClick = {})
            SinemaFilterChip(label = "Drama", selected = false, onClick = {})
        }
    }
}

/** With a leading icon and a count — the shape the search bar's entry point takes. */
@SinemaPreview
@Composable
private fun SinemaFilterChipWithCountPreview() {
    SinemaPreviewSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SinemaFilterChip(
                label = "Filters",
                selected = false,
                onClick = {},
                leading = { ChipPreviewIcon(selected = false) },
            )
            SinemaFilterChip(
                label = "Filters",
                selected = true,
                count = 3,
                onClick = {},
                leading = { ChipPreviewIcon(selected = true) },
            )
        }
    }
}

/**
 * The distinction the two chips encode: a label sized to its text, a control
 * sized to a finger. Worth seeing side by side before reaching for either.
 */
@SinemaPreview
@Composable
private fun SinemaChipVersusFilterChipPreview() {
    SinemaPreviewSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SinemaChip(label = "PG-13", selected = true, onClick = {})
            SinemaFilterChip(label = "PG-13", selected = true, onClick = {})
        }
    }
}

@SinemaFontScalePreview
@Composable
private fun SinemaFilterChipFontScalePreview() {
    SinemaPreviewSurface {
        SinemaFilterChip(label = "Documentary", selected = true, count = 2, onClick = {})
    }
}

@Composable
private fun ChipPreviewIcon(selected: Boolean) {
    Icon(
        imageVector = SinemaIcons.Filter,
        contentDescription = null,
        tint = if (selected) {
            SinemaTheme.colors.onPrimaryContainer
        } else {
            SinemaTheme.colors.textSecondary
        },
        modifier = Modifier.size(14.dp),
    )
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
