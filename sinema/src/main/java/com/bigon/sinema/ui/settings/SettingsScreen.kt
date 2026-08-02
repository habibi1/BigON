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
import com.bigon.core.designsystem.components.SinemaAttributionFooter
import com.bigon.core.designsystem.components.SinemaSegmentedControl
import com.bigon.core.designsystem.components.SinemaSettingRow
import com.bigon.core.designsystem.icons.SinemaIcons
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.sinema.BuildConfig
import com.bigon.sinema.ui.ThemeMode

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
}
