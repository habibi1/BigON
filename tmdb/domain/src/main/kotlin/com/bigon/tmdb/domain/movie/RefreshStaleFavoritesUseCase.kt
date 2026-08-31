package com.bigon.tmdb.domain.movie

import javax.inject.Inject

/**
 * Refetches favourites whose cached TMDB content has aged past the six-month
 * limit in §1.C of TMDB's API Terms of Use.
 *
 * A favourite is two things stored together: the user's decision to keep a
 * film, and a copy of TMDB's data describing it. Only the second half expires,
 * so this refreshes rather than deletes — losing someone's favourites because
 * their phone was offline would be a worse answer than the problem.
 *
 * Fetching a detail already writes the snapshot back through the repository, so
 * this only has to ask. Failures are ignored on purpose: the pass runs at launch
 * on whatever connection is available, and an id it could not reach is simply
 * still stale next time.
 */
class RefreshStaleFavoritesUseCase @Inject constructor(
    private val favorites: FavoritesRepository,
    private val movies: MovieRepository,
) {
    suspend operator fun invoke() {
        favorites.staleSnapshotIds().forEach { movieId -> movies.detail(movieId) }
    }
}
