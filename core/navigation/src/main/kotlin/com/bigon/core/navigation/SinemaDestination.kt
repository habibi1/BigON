package com.bigon.core.navigation

import kotlinx.serialization.Serializable

/**
 * Every place the app can navigate to, as typed values rather than strings.
 *
 * Routes live in their own framework-free module so feature modules can point at
 * each other's destinations without depending on each other — the rule the
 * architecture has always claimed but, until now, had no mechanism for. There
 * is deliberately no `Navigator` service here: screens raise callbacks, and the
 * single NavHost in the app shell decides what they mean.
 */
@Serializable
sealed interface SinemaDestination

/** Destinations reachable from the navigation bar / rail. */
@Serializable
sealed interface TopLevelDestination : SinemaDestination

@Serializable
data object HomeDestination : TopLevelDestination

@Serializable
data object SearchDestination : TopLevelDestination

@Serializable
data object FavoritesDestination : TopLevelDestination

@Serializable
data object SettingsDestination : TopLevelDestination

@Serializable
data class MovieDetailDestination(val movieId: Long) : SinemaDestination

/** A franchise, reached from the detail screen of any part. */
@Serializable
data class CollectionDestination(val collectionId: Long) : SinemaDestination
