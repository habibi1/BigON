package com.bigon.core.model

/**
 * The browsable lists Home offers. Each is cached separately, so loading one
 * never evicts another — the categories share a table but not a key.
 *
 * The enum lives in the domain model rather than the data layer because the UI
 * selects by category; how each maps to a TMDB endpoint stays a data concern.
 */
enum class MovieCategory(val label: String) {
    Trending("Trending"),
    Popular("Popular"),
    NowPlaying("Now Playing"),
    TopRated("Top Rated"),
    Upcoming("Upcoming"),
    ;

    companion object {
        val Default: MovieCategory = Trending
    }
}
