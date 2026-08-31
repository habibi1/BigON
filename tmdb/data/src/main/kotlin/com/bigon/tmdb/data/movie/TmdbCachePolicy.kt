package com.bigon.tmdb.data.movie

import java.time.Duration

/**
 * How long TMDB content may be held locally.
 *
 * §1.C of TMDB's API Terms of Use forbids caching their content "for longer
 * than 6 months". That is a licence condition, not a tuning parameter, so the
 * limit lives here rather than as a number repeated at each call site — and the
 * two caches that hold TMDB content both measure against it:
 *
 * - Detail payloads expire on read, so an over-age row is never served even if
 *   nothing has swept it yet.
 * - Favourite snapshots are refreshed, not discarded: a favourite is the user's
 *   own record, while the title and artwork cached alongside it are TMDB's.
 *
 * Separate from [DefaultMovieRepository.MAX_CACHED_DETAILS], which bounds the
 * cache by *size* for the user's benefit. This bounds it by *age* for TMDB's.
 */
internal object TmdbCachePolicy {

    /** The §1.C ceiling. Six 30-day months, deliberately short of the limit. */
    val MAX_AGE: Duration = Duration.ofDays(180)

    /** Content fetched before this instant may no longer be shown. */
    fun cutoff(now: Long): Long = now - MAX_AGE.toMillis()

    fun isExpired(fetchedAt: Long, now: Long): Boolean = fetchedAt < cutoff(now)
}
