package com.bigon.sinema.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
        val reselects = rememberTabReselects()

        // One wrapper is the whole in-app update integration; :core:update owns
        // both flows — the blocking screen for a forced update and the sheet for
        // an optional one. It sits outside the scaffold so a blocked build has
        // no reachable navigation bar behind the gate.
        UpdateGate {
            BigonAppScaffold(
                items = TopLevelTab.entries.map { tab ->
                    BigonNavItem(id = tab.name, label = tab.label, icon = tab.icon)
                },
                selectedId = (currentDestination.currentTab() ?: TopLevelTab.Home).name,
                onSelect = { id ->
                    val tab = TopLevelTab.valueOf(id)
                    // Tapping the open tab is a separate gesture, not a
                    // navigation: navigateToTopLevel would restore the very
                    // state the tap is asking to leave, so it is never called.
                    if (currentDestination.currentTab() == tab) {
                        reselects.reselected(tab)
                    } else {
                        navController.navigateToTopLevel(tab.destination)
                    }
                },
                // Detail is full-bleed: its backdrop runs under the status bar
                // and would fight a navigation bar.
                showNavigation = currentDestination.isTopLevel(),
            ) { contentPadding ->
                // Inside the scaffold's content slot, not around the whole
                // scaffold, and clipped to it.
                //
                // A shared element is lifted into this layout's overlay and
                // drawn above everything inside it for the length of its
                // flight. Wrapped around the scaffold, "everything" included
                // the navigation bar — so tapping a card the bar was covering
                // made its poster jump to full size over the bar, and coming
                // back it sat on top of the bar until the animation ended and
                // it snapped underneath. Confined to the content slot the
                // overlay stops at content, and the scaffold composes the bar
                // and the rail after content, so navigation still draws over
                // a flying poster.
                //
                // That ordering is what fixes it, measured: with the overlay
                // moved in here the bar strip is pixel-identical across every
                // frame of the flight, with or without the clip below. The clip
                // stays as a cheap guard on the other edges, not as the
                // mechanism.
                SharedTransitionLayout(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    SinemaNavHost(
                        navController = navController,
                        contentPadding = contentPadding,
                        reselects = reselects,
                    )
                }
            }
        }
    }
}
