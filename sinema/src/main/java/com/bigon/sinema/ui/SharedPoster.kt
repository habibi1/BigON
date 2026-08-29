package com.bigon.sinema.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.bigon.core.designsystem.theme.BigonTheme

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
 *
 * [shape] is the poster's corner rounding, and it has to be stated here rather
 * than left to the `Modifier.clip` each end already has. A shared element is
 * lifted into an overlay for the length of its flight and drawn from its own
 * node downwards — but at both ends the clip sits *above* that node, on the
 * card's poster box on one side and before `.then(posterModifier(…))` on the
 * other. So the corners went square the moment the animation started and
 * snapped back to rounded when it landed. This clips the overlay itself.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PosterTransition?.posterModifier(
    movieId: Long,
    shape: Shape = BigonTheme.shapes.card,
): Modifier =
    if (this == null) {
        Modifier
    } else {
        with(sharedScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "movie-poster-$movieId"),
                animatedVisibilityScope = animatedScope,
                clipInOverlayDuringTransition = OverlayClip(shape),
            )
        }
    }
