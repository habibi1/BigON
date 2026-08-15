package com.bigon.sinema.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bigon.core.designsystem.components.BigonAppScaffold
import com.bigon.core.designsystem.components.BigonNavItem
import com.bigon.core.designsystem.theme.BigonTheme
import com.bigon.core.update.UpdateGate

/**
 * App shell: theme, system bars, navigation bar/rail, and the navigation graph.
 *
 * Navigation is a real [androidx.navigation.compose.NavHost] with type-safe
 * destinations, so the back stack — not a nullable id in composition — decides
 * what is on screen. The selected tab is derived from the current back-stack
 * entry rather than tracked separately, which is why it survives process death
 * and can never disagree with what is displayed.
 *
 * Everything sits inside one [SharedTransitionLayout] so the poster keeps
 * animating from any grid into the detail header across that back stack.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SinemaApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }

    // Bars follow the app's theme, not the system's (see SystemBarsEffect).
    SystemBarsEffect(darkTheme = darkTheme)

    BigonTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val currentEntry by navController.currentBackStackEntryAsState()
        val currentDestination = currentEntry?.destination

        // One wrapper is the whole in-app update integration; :core:update owns
        // both flows — the blocking screen for a forced update and the sheet for
        // an optional one. It sits outside the scaffold so a blocked build has
        // no reachable navigation bar behind the gate.
        UpdateGate {
            SharedTransitionLayout {
                BigonAppScaffold(
                    items = TopLevelTab.entries.map { tab ->
                        BigonNavItem(id = tab.name, label = tab.label, icon = tab.icon)
                    },
                    selectedId = (currentDestination.currentTab() ?: TopLevelTab.Home).name,
                    onSelect = { id ->
                        navController.navigateToTopLevel(TopLevelTab.valueOf(id).destination)
                    },
                    // Detail is full-bleed: its backdrop runs under the status bar
                    // and would fight a navigation bar.
                    showNavigation = currentDestination.isTopLevel(),
                ) {
                    SinemaNavHost(navController = navController)
                }
            }
        }
    }
}
