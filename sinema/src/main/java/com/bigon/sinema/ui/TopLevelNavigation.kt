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
import androidx.compose.ui.graphics.vector.ImageVector
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
