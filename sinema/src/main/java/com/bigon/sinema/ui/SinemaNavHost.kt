package com.bigon.sinema.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bigon.core.navigation.FavoritesDestination
import com.bigon.core.navigation.HomeDestination
import com.bigon.core.navigation.CollectionDestination
import com.bigon.core.navigation.MovieDetailDestination
import com.bigon.core.navigation.SearchDestination
import com.bigon.core.navigation.SettingsDestination
import com.bigon.sinema.ui.collection.CollectionRoute
import com.bigon.sinema.ui.detail.DetailRoute
import com.bigon.sinema.ui.favorites.FavoritesRoute
import com.bigon.sinema.ui.home.HomeRoute
import com.bigon.sinema.ui.search.SearchRoute
import com.bigon.sinema.ui.settings.SettingsRoute

/** Matches the poster hand-off, so the fade never outlives the shared element. */
private const val TRANSITION_MILLIS = 380

/**
 * The app's single navigation graph, using Navigation Compose's type-safe
 * routes: destinations are `@Serializable` values from :core:navigation, so a
 * movie id travels as a `Long` rather than a stringly-typed argument, and a
 * typo is a compile error instead of a blank screen.
 *
 * It sits *inside* the shared-transition layout, which is what lets the poster
 * animate from any grid into the detail header across a real back stack.
 * Screens raise callbacks; deciding what each one means belongs here alone.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SinemaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier,
        // Content cross-fades; the poster is animated by the shared element
        // itself, so a sliding transition would fight it.
        enterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MILLIS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        popExitTransition = { fadeOut(tween(TRANSITION_MILLIS)) },
    ) {
        composable<HomeDestination> {
            HomeRoute(
                onMovieClick = { navController.navigateToMovie(it) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
            )
        }

        composable<SearchDestination> {
            SearchRoute(
                onMovieClick = { navController.navigateToMovie(it) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
            )
        }

        composable<FavoritesDestination> {
            FavoritesRoute(
                onMovieClick = { navController.navigateToMovie(it) },
                onBrowseTrending = { navController.navigateToTopLevel(HomeDestination) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
            )
        }

        composable<SettingsDestination> {
            SettingsRoute()
        }

        composable<MovieDetailDestination> { entry ->
            val destination = entry.toRoute<MovieDetailDestination>()
            DetailRoute(
                movieId = destination.movieId,
                onBack = { navController.popBackStack() },
                // Detail can open detail. Each tap pushes a new entry rather
                // than replacing this one, so back walks the trail the user
                // actually followed instead of jumping to the grid.
                onMovieClick = { navController.navigateToMovie(it) },
                onCollectionClick = { navController.navigate(CollectionDestination(it)) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
            )
        }

        composable<CollectionDestination> { entry ->
            CollectionRoute(
                collectionId = entry.toRoute<CollectionDestination>().collectionId,
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigateToMovie(it) },
            )
        }
    }
}

private fun NavHostController.navigateToMovie(movieId: Long) {
    navigate(MovieDetailDestination(movieId))
}
