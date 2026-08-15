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
import com.bigon.tmdb.ui.SinemaAttributionFooter
import com.bigon.core.designsystem.components.SinemaSegmentedControl
import com.bigon.core.designsystem.components.SinemaSettingRow
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
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
import com.bigon.core.designsystem.components.SinemaLoadingIndicator
import com.bigon.core.designsystem.components.SinemaSearchBar
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
    val spacing = SinemaTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = spacing.l),
    ) {
        Text(
            text = "Settings",
            style = SinemaTheme.typography.display,
            color = SinemaTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.l),
        )

        Text(
            text = "Theme",
            style = SinemaTheme.typography.title,
            color = SinemaTheme.colors.textPrimary,
        )
        SinemaSegmentedControl(
            options = ThemeMode.entries.map { it.name },
            selectedIndex = state.themeMode.ordinal,
            onSelect = { onIntent(SettingsIntent.ThemeChanged(ThemeMode.entries[it])) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.m),
        )

        Column(modifier = Modifier.padding(top = spacing.xl)) {
            SinemaSettingRow(
                title = "Region",
                subtitle = "Cinema listings, age ratings and streaming services",
                value = state.regionLabel,
                icon = SinemaIcons.Search,
                onClick = { onIntent(SettingsIntent.RegionPickerOpened) },
            )
            SinemaSettingRow(
                title = "Clear cache",
                subtitle = if (state.isClearingCache) "Clearing…" else "Catalogue and images — favorites kept",
                value = state.cacheLabel,
                icon = SinemaIcons.Clear,
                onClick = { onIntent(SettingsIntent.ClearCache) },
            )
            SinemaSettingRow(
                title = "App version",
                subtitle = "Sinema for Android",
                value = BuildConfig.VERSION_NAME,
                icon = SinemaIcons.Movie,
                showDivider = false,
            )
        }

        SinemaAttributionFooter(
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
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors

    Dialog(onDismissRequest = { onIntent(SettingsIntent.RegionPickerDismissed) }) {
        Surface(
            shape = SinemaTheme.shapes.container,
            color = colors.surface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        ) {
            Column(modifier = Modifier.padding(spacing.l)) {
                Text(
                    text = "Content region",
                    style = SinemaTheme.typography.title,
                    color = colors.textPrimary,
                )

                SinemaSearchBar(
                    query = state.regionQuery,
                    onQueryChange = { onIntent(SettingsIntent.RegionQueryChanged(it)) },
                    placeholder = "Search regions…",
                    modifier = Modifier.padding(top = spacing.m),
                )

                when {
                    state.isLoadingRegions -> Box(
                        modifier = Modifier.fillMaxWidth().padding(spacing.xl),
                        contentAlignment = Alignment.Center,
                    ) { SinemaLoadingIndicator() }

                    state.regionError != null -> Text(
                        text = state.regionError.asString(),
                        style = SinemaTheme.typography.body,
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
    val spacing = SinemaTheme.spacing
    val colors = SinemaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(SinemaTheme.shapes.container)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.m, horizontal = spacing.s),
    ) {
        Text(
            text = label,
            style = SinemaTheme.typography.body,
            color = if (selected) colors.primary else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                style = SinemaTheme.typography.caption,
                color = colors.textSecondary,
            )
        }
    }
}
