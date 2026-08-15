package com.bigon.tmdb.data.movie

import com.bigon.tmdb.model.CastMember
import com.bigon.tmdb.model.CollectionRef
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieDetail
import com.bigon.tmdb.model.WatchProvider
import com.bigon.tmdb.model.WatchProviders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * The on-disk form of [MovieDetail].
 *
 * This exists rather than annotating the domain model because :core:model is
 * the one type every layer shares — putting a storage concern on it would make
 * the persistence format part of the domain contract, and the next storage
 * decision would have to be litigated there too.
 *
 * It is also deliberately *not* the API response. Caching the raw TMDB payload
 * would store ~224 KB per title, most of it regions and crew the app discards;
 * this snapshot is the trimmed result and roughly an order of magnitude
 * smaller. Field names are short for the same reason.
 */
@Serializable
internal data class MovieDetailSnapshot(
    @SerialName("i") val id: Long,
    @SerialName("t") val title: String,
    @SerialName("tg") val tagline: String? = null,
    @SerialName("o") val overview: String = "",
    @SerialName("p") val posterUrl: String? = null,
    @SerialName("b") val backdropUrl: String? = null,
    @SerialName("d") val releaseDate: String? = null,
    @SerialName("v") val voteAverage: Double? = null,
    @SerialName("vc") val voteCount: Int = 0,
    @SerialName("r") val runtimeMinutes: Int? = null,
    @SerialName("g") val genres: List<String> = emptyList(),
    @SerialName("c") val cast: List<CastSnapshot> = emptyList(),
    @SerialName("tk") val trailerKey: String? = null,
    @SerialName("rec") val recommendations: List<MovieSnapshot> = emptyList(),
    @SerialName("cert") val certification: String? = null,
    @SerialName("kw") val keywords: List<String> = emptyList(),
    @SerialName("imdb") val imdbId: String? = null,
    @SerialName("wp") val watchProviders: WatchProvidersSnapshot? = null,
    @SerialName("lg") val logoUrl: String? = null,
    @SerialName("alt") val alternativeTitles: List<String> = emptyList(),
    @SerialName("loc") val isLocalised: Boolean = false,
    @SerialName("colId") val collectionId: Long? = null,
    @SerialName("colName") val collectionName: String? = null,
)

@Serializable
internal data class CastSnapshot(
    @SerialName("i") val id: Long,
    @SerialName("n") val name: String,
    @SerialName("ch") val character: String,
    @SerialName("pr") val profileUrl: String? = null,
)

@Serializable
internal data class MovieSnapshot(
    @SerialName("i") val id: Long,
    @SerialName("t") val title: String,
    @SerialName("o") val overview: String = "",
    @SerialName("p") val posterUrl: String? = null,
    @SerialName("b") val backdropUrl: String? = null,
    @SerialName("d") val releaseDate: String? = null,
    @SerialName("v") val voteAverage: Double? = null,
    @SerialName("g") val genres: List<String> = emptyList(),
)

@Serializable
internal data class WatchProvidersSnapshot(
    @SerialName("rg") val region: String,
    @SerialName("l") val link: String? = null,
    @SerialName("s") val streaming: List<ProviderSnapshot> = emptyList(),
    @SerialName("rt") val rent: List<ProviderSnapshot> = emptyList(),
    @SerialName("by") val buy: List<ProviderSnapshot> = emptyList(),
)

@Serializable
internal data class ProviderSnapshot(
    @SerialName("i") val id: Int,
    @SerialName("n") val name: String,
    @SerialName("lg") val logoUrl: String? = null,
)

// ── domain ⇄ snapshot ───────────────────────────────────────────────────────

internal fun MovieDetail.toSnapshot(): MovieDetailSnapshot = MovieDetailSnapshot(
    id = id,
    title = title,
    tagline = tagline,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate?.toString(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    runtimeMinutes = runtimeMinutes,
    genres = genres,
    cast = cast.map { CastSnapshot(it.id, it.name, it.character, it.profileUrl) },
    trailerKey = trailerKey,
    recommendations = recommendations.map {
        MovieSnapshot(
            id = it.id,
            title = it.title,
            overview = it.overview,
            posterUrl = it.posterUrl,
            backdropUrl = it.backdropUrl,
            releaseDate = it.releaseDate?.toString(),
            voteAverage = it.voteAverage,
            genres = it.genres,
        )
    },
    certification = certification,
    keywords = keywords,
    imdbId = imdbId,
    logoUrl = logoUrl,
    alternativeTitles = alternativeTitles,
    isLocalised = isLocalised,
    collectionId = collection?.id,
    collectionName = collection?.name,
    watchProviders = watchProviders?.let { wp ->
        WatchProvidersSnapshot(
            region = wp.region,
            link = wp.link,
            streaming = wp.streaming.map { ProviderSnapshot(it.id, it.name, it.logoUrl) },
            rent = wp.rent.map { ProviderSnapshot(it.id, it.name, it.logoUrl) },
            buy = wp.buy.map { ProviderSnapshot(it.id, it.name, it.logoUrl) },
        )
    },
)

internal fun MovieDetailSnapshot.toDomain(): MovieDetail = MovieDetail(
    id = id,
    title = title,
    tagline = tagline,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate.toLocalDateOrNull(),
    voteAverage = voteAverage,
    voteCount = voteCount,
    runtimeMinutes = runtimeMinutes,
    genres = genres,
    cast = cast.map { CastMember(it.id, it.name, it.character, it.profileUrl) },
    trailerKey = trailerKey,
    recommendations = recommendations.map {
        Movie(
            id = it.id,
            title = it.title,
            overview = it.overview,
            posterUrl = it.posterUrl,
            backdropUrl = it.backdropUrl,
            releaseDate = it.releaseDate.toLocalDateOrNull(),
            voteAverage = it.voteAverage,
            genres = it.genres,
        )
    },
    certification = certification,
    keywords = keywords,
    imdbId = imdbId,
    logoUrl = logoUrl,
    alternativeTitles = alternativeTitles,
    isLocalised = isLocalised,
    collection = collectionId?.let { id -> collectionName?.let { CollectionRef(id, it) } },
    watchProviders = watchProviders?.let { wp ->
        WatchProviders(
            region = wp.region,
            link = wp.link,
            streaming = wp.streaming.map { WatchProvider(it.id, it.name, it.logoUrl) },
            rent = wp.rent.map { WatchProvider(it.id, it.name, it.logoUrl) },
            buy = wp.buy.map { WatchProvider(it.id, it.name, it.logoUrl) },
        )
    },
)

/** Dates round-trip as ISO text; a corrupt value degrades to null, not a crash. */
private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    }
