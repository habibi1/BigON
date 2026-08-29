package com.bigon.sinema.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bigon.sinema.navigation.FavoritesDestination
import com.bigon.sinema.navigation.HomeDestination
import com.bigon.sinema.navigation.CollectionDestination
import com.bigon.sinema.navigation.MovieDetailDestination
import com.bigon.sinema.navigation.PersonDestination
import com.bigon.sinema.navigation.TvDetailDestination
import com.bigon.sinema.navigation.SearchDestination
import com.bigon.sinema.navigation.SettingsDestination
import com.bigon.sinema.ui.collection.CollectionRoute
import com.bigon.sinema.ui.detail.DetailRoute
import com.bigon.sinema.ui.person.PersonRoute
import com.bigon.sinema.ui.tv.TvDetailRoute
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
    /** Space the floating bar or rail covers; only top-level screens need it. */
    contentPadding: PaddingValues = PaddingValues(),
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
                onTvClick = { navController.navigate(TvDetailDestination(it)) },
                onPersonClick = { navController.navigate(PersonDestination(it)) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
                contentPadding = contentPadding,
            )
        }

        composable<SearchDestination> {
            SearchRoute(
                onMovieClick = { navController.navigateToMovie(it) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
                contentPadding = contentPadding,
            )
        }

        composable<FavoritesDestination> {
            FavoritesRoute(
                onMovieClick = { navController.navigateToMovie(it) },
                onBrowseTrending = { navController.navigateToTopLevel(HomeDestination) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
                contentPadding = contentPadding,
            )
        }

        composable<SettingsDestination> {
            SettingsRoute(contentPadding = contentPadding)
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
                onPersonClick = { navController.navigate(PersonDestination(it)) },
                onCollectionClick = { navController.navigate(CollectionDestination(it)) },
                transition = PosterTransition(this@SinemaNavHost, this@composable),
            )
        }

        composable<PersonDestination> { entry ->
            PersonRoute(
                personId = entry.toRoute<PersonDestination>().personId,
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigateToMovie(it) },
            )
        }

        composable<CollectionDestination> { entry ->
            CollectionRoute(
                collectionId = entry.toRoute<CollectionDestination>().collectionId,
                onBack = { navController.popBackStack() },
                onMovieClick = { navController.navigateToMovie(it) },
            )
        }

        composable<TvDetailDestination> { entry ->
            TvDetailRoute(
                tvId = entry.toRoute<TvDetailDestination>().tvId,
                onBack = { navController.popBackStack() },
                onPersonClick = { navController.navigate(PersonDestination(it)) },
            )
        }
    }
}

private fun NavHostController.navigateToMovie(movieId: Long) {
    navigate(MovieDetailDestination(movieId))
}
