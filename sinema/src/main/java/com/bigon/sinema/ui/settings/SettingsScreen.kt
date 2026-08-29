package com.bigon.sinema.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigon.tmdb.ui.BigonAttributionFooter
import com.bigon.core.designsystem.components.BigonSegmentedControl
import com.bigon.core.designsystem.components.BigonSettingRow
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.sinema.BuildConfig
import com.bigon.sinema.ui.ThemeMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import com.bigon.core.designsystem.components.BigonLoadingIndicator
import com.bigon.core.designsystem.components.BigonSearchBar
import com.bigon.core.ui.asString

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BigonTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = spacing.l),
    ) {
        Text(
            text = "Settings",
            style = BigonTheme.typography.display,
            color = BigonTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.l),
        )

        Text(
            text = "Theme",
            style = BigonTheme.typography.title,
            color = BigonTheme.colors.textPrimary,
        )
        BigonSegmentedControl(
            options = ThemeMode.entries.map { it.name },
            selectedIndex = state.themeMode.ordinal,
            onSelect = { onIntent(SettingsIntent.ThemeChanged(ThemeMode.entries[it])) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.m),
        )

        Column(modifier = Modifier.padding(top = spacing.xl)) {
            BigonSettingRow(
                title = "Region",
                // Shorter than it was: beside a value as long as "United States
                // of America" this line has about half the row, and the old
                // wording broke into three ragged fragments there.
                subtitle = "Cinema listings, ratings and streaming",
                value = state.regionLabel,
                icon = BigonIcons.Search,
                onClick = { onIntent(SettingsIntent.RegionPickerOpened) },
            )
            BigonSettingRow(
                title = "Clear cache",
                subtitle = if (state.isClearingCache) "Clearing…" else "Catalogue and images — favorites kept",
                value = state.cacheLabel,
                icon = BigonIcons.Clear,
                onClick = { onIntent(SettingsIntent.ClearCache) },
            )
            BigonSettingRow(
                title = "App version",
                subtitle = "Sinema for Android",
                value = BuildConfig.VERSION_NAME,
                icon = BigonIcons.Movie,
                showDivider = false,
            )
        }

        BigonAttributionFooter(
            versionLabel = "Sinema v${BuildConfig.VERSION_NAME}",
            modifier = Modifier.padding(vertical = spacing.xxl),
        )
    }

    if (state.isRegionPickerOpen) {
        RegionPicker(state = state, onIntent = onIntent)
    }
}

/**
 * 139 regions is too many to scan, so the picker is searchable — by name or by
 * code, because someone who knows they want "ID" should not have to remember
 * that TMDB calls it Indonesia.
 *
 * "Follow device" is pinned at the top rather than sorted in, since it is the
 * only entry that behaves differently: it keeps tracking the phone instead of
 * fixing a value.
 */
@Composable
private fun RegionPicker(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors

    Dialog(onDismissRequest = { onIntent(SettingsIntent.RegionPickerDismissed) }) {
        Surface(
            shape = BigonTheme.shapes.container,
            color = colors.surface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        ) {
            Column(modifier = Modifier.padding(spacing.l)) {
                Text(
                    text = "Content region",
                    style = BigonTheme.typography.title,
                    color = colors.textPrimary,
                )

                BigonSearchBar(
                    query = state.regionQuery,
                    onQueryChange = { onIntent(SettingsIntent.RegionQueryChanged(it)) },
                    placeholder = "Search regions…",
                    modifier = Modifier.padding(top = spacing.m),
                )

                when {
                    state.isLoadingRegions -> Box(
                        modifier = Modifier.fillMaxWidth().padding(spacing.xl),
                        contentAlignment = Alignment.Center,
                    ) { BigonLoadingIndicator() }

                    state.regionError != null -> Text(
                        text = state.regionError.asString(),
                        style = BigonTheme.typography.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.l),
                    )

                    else -> LazyColumn(modifier = Modifier.padding(top = spacing.m)) {
                        if (state.regionQuery.isBlank()) {
                            item {
                                RegionRow(
                                    label = "Follow device",
                                    trailing = state.activeRegion.takeIf { state.chosenRegion == null },
                                    selected = state.chosenRegion == null,
                                    onClick = { onIntent(SettingsIntent.RegionSelected(null)) },
                                )
                            }
                        }
                        items(state.visibleRegions, key = { it.code }) { region ->
                            RegionRow(
                                label = region.name,
                                trailing = region.code,
                                selected = region.code == state.chosenRegion,
                                onClick = { onIntent(SettingsIntent.RegionSelected(region.code)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionRow(
    label: String,
    trailing: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = BigonTheme.spacing
    val colors = BigonTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(BigonTheme.shapes.container)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.m, horizontal = spacing.s),
    ) {
        Text(
            text = label,
            style = BigonTheme.typography.body,
            color = if (selected) colors.primary else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                style = BigonTheme.typography.caption,
                color = colors.textSecondary,
            )
        }
    }
}
