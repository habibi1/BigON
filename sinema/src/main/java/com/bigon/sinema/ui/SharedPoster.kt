package com.bigon.sinema.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.lookaheadScopeCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
 * The rectangle a poster is allowed to occupy while it is in flight.
 *
 * A shared element is lifted out of the layout and drawn in an overlay above
 * everything for the length of its animation, which is what lets it cross from
 * a grid card to a detail header. The cost is that it also escapes whatever was
 * clipping it: a card scrolled half under the chip row is clipped by the grid
 * on every frame *except* the ones where it is flying, so returning to it drew
 * the whole poster across the header and then snapped it back under.
 *
 * Recording the grid's rect gives the overlay something to clip against, so the
 * poster stays inside the list it is returning to.
 */
@Stable
class PosterViewport internal constructor() {
    internal var bounds: Rect? by mutableStateOf(null)
}

@Composable
fun rememberPosterViewport(): PosterViewport = remember { PosterViewport() }

/**
 * Records this layout as [viewport]'s rect, in the shared-transition scope's own
 * coordinate space — the space [SharedTransitionScope.OverlayClip] paths have to
 * be expressed in, and not the same as the window's once anything offsets the
 * scope.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.posterViewport(
    transition: PosterTransition?,
    viewport: PosterViewport,
): Modifier =
    if (transition == null) {
        this
    } else {
        onGloballyPositioned { coordinates ->
            val scope = transition.sharedScope.lookaheadScopeCoordinates(coordinates)
            viewport.bounds = scope.localBoundingBoxOf(coordinates, clipBounds = false)
        }
    }

/**
 * The poster's rounded rect, intersected with the list it belongs to.
 *
 * Both halves matter and neither is optional: without the shape the corners go
 * square mid-flight, and without the viewport the poster paints over the header.
 * A null [viewport] means there is no list to stay inside — the detail screen —
 * and only the shape applies.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private class PosterOverlayClip(
    private val shape: Shape,
    private val viewport: PosterViewport?,
) : SharedTransitionScope.OverlayClip {
    // Reused across frames: getClipPath runs on every frame of every flight, and
    // the API asks implementations not to allocate here.
    private val posterPath = Path()
    private val viewportPath = Path()
    private val clipped = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path {
        posterPath.reset()
        posterPath.addOutline(shape.createOutline(bounds.size, layoutDirection, density))
        posterPath.translate(bounds.topLeft)

        val visible = viewport?.bounds ?: return posterPath

        viewportPath.reset()
        viewportPath.addRect(visible)
        clipped.reset()
        clipped.op(posterPath, viewportPath, PathOperation.Intersect)
        return clipped
    }
}

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
    viewport: PosterViewport? = null,
): Modifier =
    if (this == null) {
        Modifier
    } else {
        with(sharedScope) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "movie-poster-$movieId"),
                animatedVisibilityScope = animatedScope,
                clipInOverlayDuringTransition = remember(shape, viewport) {
                    PosterOverlayClip(shape, viewport)
                },
            )
        }
    }
