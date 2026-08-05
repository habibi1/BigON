package com.bigon.sinema.ui

import com.bigon.core.model.Movie

/**
 * "2026 · Sci-Fi", degrading gracefully when either half is missing.
 *
 * Shared rather than duplicated per screen: the card meta line is the same
 * affordance everywhere a poster appears, and two copies would drift the first
 * time one of them gained a separator or a third field.
 */
internal fun Movie.metaLine(): String? =
    listOfNotNull(releaseYear?.toString(), genres.firstOrNull())
        .joinToString(" · ")
        .takeIf { it.isNotBlank() }
