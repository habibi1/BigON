package com.bigon.sinema.ui.trailer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens a trailer in YouTube.
 *
 * Playback is YouTube's, not ours. Every video TMDB returns is hosted there,
 * and their API Services Developer Policies forbid separating the audio or
 * video components from the embedded player (I.7) or caching copies (E.1.a) —
 * so the realistic choices are an embedded player or a hand-off. This is the
 * hand-off, and it is the honest one: the trailer plays in the app that owns
 * it, with the user's own account, quality settings and captions.
 *
 * Two intents, in order of preference:
 *
 *  1. `vnd.youtube:<id>` — the YouTube app's own scheme. Opens the video
 *     directly, no chooser, no browser round-trip.
 *  2. the watch URL — resolves to YouTube if installed, otherwise a browser.
 *     Also what a device with no YouTube app gets.
 *
 * Returns false only when neither can be started, which in practice means a
 * device with no browser and no YouTube — rare, but the caller still has to say
 * something rather than appear to do nothing.
 */
fun Context.openTrailer(videoKey: String): Boolean {
    if (videoKey.isBlank()) return false

    val app = Intent(Intent.ACTION_VIEW, "vnd.youtube:$videoKey".toUri())
    val web = Intent(Intent.ACTION_VIEW, watchUrl(videoKey).toUri())

    // FLAG_ACTIVITY_NEW_TASK so the trailer lands in its own task: coming back
    // should return to the detail screen, not drop YouTube into Sinema's stack.
    for (intent in listOf(app, web)) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the next one; the web URL is the last resort.
        }
    }
    return false
}

/** The canonical watch URL, also used for sharing. */
fun watchUrl(videoKey: String): String = "https://www.youtube.com/watch?v=$videoKey"
