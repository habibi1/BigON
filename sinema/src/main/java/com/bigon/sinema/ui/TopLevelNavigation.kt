package com.bigon.sinema.ui

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.bigon.core.designsystem.icons.BigonIcons
import com.bigon.sinema.navigation.FavoritesDestination
import com.bigon.sinema.navigation.HomeDestination
import com.bigon.sinema.navigation.SearchDestination
import com.bigon.sinema.navigation.SettingsDestination
import com.bigon.sinema.navigation.TopLevelDestination
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * The bar/rail entries. Icons live here rather than in :core:navigation, which
 * must stay free of Compose so it can be shared by non-UI modules.
 */
enum class TopLevelTab(
    val destination: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
) {
    Home(HomeDestination, "Home", BigonIcons.Home),
    Search(SearchDestination, "Search", BigonIcons.Search),
    Favorites(FavoritesDestination, "Favorites", BigonIcons.HeartOutline),
    Settings(SettingsDestination, "Settings", BigonIcons.Settings),
    ;

    val routeClass: KClass<out TopLevelDestination> get() = destination::class
}

/** True while the given back-stack destination belongs to a bar entry. */
fun NavDestination?.isTopLevel(): Boolean =
    TopLevelTab.entries.any { tab -> this?.hierarchy()?.any { it.hasRoute(tab.routeClass) } == true }

fun NavDestination?.currentTab(): TopLevelTab? =
    TopLevelTab.entries.firstOrNull { tab -> this?.hierarchy()?.any { it.hasRoute(tab.routeClass) } == true }

private fun NavDestination.hierarchy(): Sequence<NavDestination> =
    generateSequence(this) { it.parent }

/**
 * Standard bottom-navigation semantics: one entry per tab on the back stack,
 * state preserved per tab, and no growing stack of repeated taps.
 */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Taps on the tab that is already showing.
 *
 * Navigating to where you already are is a no-op, so the bar's usual signal —
 * a destination change — says nothing here. This carries the tap itself, which
 * is the only way the screen can learn that anything happened.
 *
 * A shared flow rather than state: "tapped again" is an event, and two taps in
 * a row have to read as two. Buffered by one and dropping the oldest on
 * overflow, so an impatient double-tap while a screen is not collecting cannot
 * queue up a second scroll to run after the first.
 */
@Stable
class TabReselects internal constructor() {
    private val _events = MutableSharedFlow<TopLevelTab>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    internal fun reselected(tab: TopLevelTab) {
        _events.tryEmit(tab)
    }

    /** Emits every time [tab] is tapped while it is already the open tab. */
    internal fun forTab(tab: TopLevelTab): Flow<Unit> =
        _events.asSharedFlow().filter { it == tab }.map { }
}

@Composable
fun rememberTabReselects(): TabReselects = remember { TabReselects() }

/**
 * The stream a screen collects to know its own tab was tapped again. Remembered
 * so collection is not restarted on every recomposition.
 */
@Composable
fun TabReselects?.forTabOrNull(tab: TopLevelTab): Flow<Unit>? =
    this?.let { reselects -> remember(reselects, tab) { reselects.forTab(tab) } }
