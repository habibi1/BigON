package com.bigon.core.model

/** A TMDB genre. Chips select by [id]; movies carry resolved names. */
data class Genre(
    val id: Int,
    val name: String,
)
