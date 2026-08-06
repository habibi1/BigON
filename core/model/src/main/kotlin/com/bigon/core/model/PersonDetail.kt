package com.bigon.core.model

import java.time.LocalDate

/**
 * A person and the films they are credited on.
 *
 * [filmography] merges cast and crew credits: users think in "things this
 * person made", not in TMDB's department split, and a director who also acted
 * would otherwise appear in two disconnected lists.
 */
data class PersonDetail(
    val id: Long,
    val name: String,
    val biography: String,
    val profileUrl: String?,
    val knownForDepartment: String?,
    val birthday: LocalDate?,
    val deathday: LocalDate?,
    val placeOfBirth: String?,
    val filmography: List<Credit>,
) {
    /** Living people have no death date; the UI must not print a dangling dash. */
    val lifespan: String?
        get() = when {
            birthday == null -> null
            deathday == null -> "${birthday.year}"
            else -> "${birthday.year} – ${deathday.year}"
        }
}

/**
 * One film credit. [role] is the character for a cast credit and the job for a
 * crew one — the distinction the user cares about is what they *did*, not which
 * array TMDB returned it in.
 */
data class Credit(
    val movie: Movie,
    val role: String?,
)
