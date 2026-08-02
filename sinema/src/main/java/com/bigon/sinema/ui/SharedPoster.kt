package com.bigon.sinema.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Carries the two scopes a shared-element transition needs from the app shell
 * down to whichever screen draws the poster.
 *
 * Nullable everywhere it is consumed: previews and tests render the same
 * composables without a [SharedTransitionScope], and simply get no animation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
data class PosterTransition(
    val sharedScope: SharedTransitionScope,
    val animatedScope: AnimatedVisibilityScope,
)

/**
 * The poster on the grid card and the poster on the detail screen share this
 * key, which is what lets Compose animate one into the other instead of
 * cross-fading two unrelated images.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PosterTransition?.posterModifier(movieId: Long): Modifier =
    if (this == null) {
        Modifier
    } else {
        with(sharedScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "movie-poster-$movieId"),
                animatedVisibilityScope = animatedScope,
            )
        }
    }
