package com.bigon.tmdb.model

/**
 * One video TMDB holds for a title — a trailer, teaser, clip or featurette.
 *
 * [key] is a YouTube video id, not a URL and not a stream. Every video TMDB
 * returns is hosted on YouTube (88 of 88 for one large title, checked against
 * the live API), so playback means embedding YouTube's own player: their API
 * Services Developer Policies I.7 forbid separating the audio or video
 * components from it, and E.1.a forbids caching copies.
 */
data class Video(
    val key: String,
    val name: String,
    val type: VideoType,
    val isOfficial: Boolean,
    /** Vertical resolution TMDB reports, e.g. 1080. Not a file size. */
    val sizePx: Int,
    /** ISO-8601 instant, kept as text — only ever used for ordering. */
    val publishedAt: String?,
)

enum class VideoType {
    Trailer,
    Teaser,
    Clip,
    Featurette,
    BehindTheScenes,
    Bloopers,
    Other,
    ;

    companion object {
        fun from(raw: String): VideoType = when (raw.lowercase()) {
            "trailer" -> Trailer
            "teaser" -> Teaser
            "clip" -> Clip
            "featurette" -> Featurette
            "behind the scenes" -> BehindTheScenes
            "bloopers" -> Bloopers
            else -> Other
        }
    }
}
