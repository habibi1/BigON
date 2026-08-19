package com.bigon.tmdb.model

import java.time.LocalDate

/**
 * One entry in the mixed trending feed, which returns films, series and people
 * interleaved behind a `media_type` discriminator.
 *
 * A sealed type rather than one wide class with nullable fields: a series has
 * no runtime and a person has no rating, and modelling that as "null" pushes
 * the question of which fields are meaningful onto every call site. Here the
 * compiler answers it.
 *
 * Only [Film] has a destination inside the app today. Series and people are
 * shown but open TMDB's own page — their native screens are Tier 3 work, and a
 * card that silently does nothing when tapped is worse than one that goes
 * somewhere honest.
 */
sealed interface TrendingItem {

    val id: Long

    /** What the card shows as its heading, whatever the type. */
    val title: String

    /** Poster or portrait; null often enough that the UI must handle it. */
    val imageUrl: String?

    /** TMDB's own page, used until a native screen exists for this type. */
    val tmdbUrl: String

    /**
     * A film, carrying the same [Movie] the rest of the app already speaks, so
     * the existing card and detail route work unchanged.
     */
    data class Film(val movie: Movie) : TrendingItem {
        override val id: Long get() = movie.id
        override val title: String get() = movie.title
        override val imageUrl: String? get() = movie.posterUrl
        override val tmdbUrl: String get() = "https://www.themoviedb.org/movie/$id"
    }

    data class Series(
        override val id: Long,
        val name: String,
        val overview: String,
        val posterUrl: String?,
        val firstAirDate: LocalDate?,
        val voteAverage: Double?,
        val genres: List<String>,
    ) : TrendingItem {
        override val title: String get() = name
        override val imageUrl: String? get() = posterUrl
        override val tmdbUrl: String get() = "https://www.themoviedb.org/tv/$id"
        val firstAirYear: Int? get() = firstAirDate?.year
    }

    data class Person(
        override val id: Long,
        val name: String,
        val profileUrl: String?,
        /** "Acting", "Directing" — TMDB's own department label. */
        val knownForDepartment: String?,
        /** Titles they are known for; frequently empty, so never load-bearing. */
        val knownFor: List<String>,
    ) : TrendingItem {
        override val title: String get() = name
        override val imageUrl: String? get() = profileUrl
        override val tmdbUrl: String get() = "https://www.themoviedb.org/person/$id"
    }
}

/** The label a mixed feed puts on a card so the type is never ambiguous. */
val TrendingItem.typeLabel: String
    get() = when (this) {
        is TrendingItem.Film -> "Film"
        is TrendingItem.Series -> "Series"
        is TrendingItem.Person -> "Person"
    }
