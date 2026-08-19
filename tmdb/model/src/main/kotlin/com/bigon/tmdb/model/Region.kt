package com.bigon.tmdb.model

/**
 * A content region the app can meaningfully use.
 *
 * Sourced from TMDB's watch-provider region list (139 entries) rather than the
 * ISO country list (251): the extra 112 are countries TMDB holds no data for,
 * and offering them would let a user pick a region that silently makes the app
 * worse.
 */
data class Region(
    /** ISO-3166-1 alpha-2, uppercase. */
    val code: String,
    val name: String,
)
