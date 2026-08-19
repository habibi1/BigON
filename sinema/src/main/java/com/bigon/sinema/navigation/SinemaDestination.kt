package com.bigon.sinema.navigation

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

/**
 * The three content domains Tier 3 added.
 *
 * Each carries its own id type rather than a shared "contentId", because TMDB
 * ids are unique only *within* a domain — film 550 and series 550 are different
 * things, and one destination would make that collision expressible.
 */
@Serializable
data class PersonDestination(val personId: Long) : SinemaDestination

@Serializable
data class CollectionDestination(val collectionId: Long) : SinemaDestination

@Serializable
data class TvDetailDestination(val tvId: Long) : SinemaDestination
