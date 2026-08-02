package com.bigon.data.movie

/**
 * Turns TMDB path fragments (`/abc.jpg`) into loadable URLs.
 *
 * TMDB documents `/configuration` as the source of the base URL and size list.
 * It has been stable for years, so the values are inlined and the endpoint is
 * skipped — one fewer request on every cold start, at the cost of a constant to
 * update if TMDB ever moves the CDN.
 */
object TmdbImageUrl {

    private const val BASE = "https://image.tmdb.org/t/p/"

    /** Sizes chosen to match how the design system renders each image. */
    const val POSTER_CARD = "w342"
    const val BACKDROP_WIDE = "w780"
    const val PROFILE_SMALL = "w185"

    fun build(path: String?, size: String): String? =
        path?.takeIf { it.isNotBlank() }?.let { "$BASE$size${it.ensureLeadingSlash()}" }

    private fun String.ensureLeadingSlash(): String = if (startsWith("/")) this else "/$this"
}
