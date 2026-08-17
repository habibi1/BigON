package com.bigon.core.designsystem.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether this composition is being drawn on a television.
 *
 * Read from [Configuration.uiMode] rather than from a `PackageManager` feature
 * query, for two reasons. The Konsist rule in `ArchitectureTest` keeps
 * `android.content.Context` out of the design system, and a feature query would
 * need one. More usefully, `uiMode` is part of the configuration, so it survives
 * a configuration change and works in previews and Robolectric — a
 * `hasSystemFeature` call answers the same question but only on a real device.
 *
 * This is a form-factor switch, not a size class. `BigonAppScaffold` already
 * adapts to width, and a television is not simply a very wide tablet: it is
 * operated from three metres away with a directional pad, which changes type
 * size, focus visibility and safe margins independently of how many dp it has.
 */
val LocalBigonIsTelevision = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
internal fun isTelevisionConfiguration(): Boolean =
    LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION

/**
 * Overscan margin. Television panels may crop the outer edge of the signal, and
 * how much varies by set, so the platform guidance is to keep content inside 5%
 * of each edge rather than to detect it. At 1080p that is the 48dp / 27dp below.
 *
 * Applied only on a television: on a phone this padding would be a wide, empty
 * frame around content that is already inset by the system bars.
 */
val BigonOverscan = PaddingValues(horizontal = 48.dp, vertical = 27.dp)

/** Inset content by [BigonOverscan], but only where a panel might crop it. */
@Composable
fun Modifier.overscanPadding(): Modifier =
    if (LocalBigonIsTelevision.current) padding(BigonOverscan) else this

/**
 * Minimum poster width for an adaptive browse grid.
 *
 * 120dp is right for a thumb on a phone. Left alone on a television it produces
 * roughly eight columns of small posters, which is wrong twice over: the art is
 * too small to read across a room, and crossing eight columns with a directional
 * pad takes eight presses where a phone took one flick. Fewer, larger cells are
 * both more legible and fewer keystrokes away.
 */
val posterGridMinWidth: Dp
    @Composable get() = if (LocalBigonIsTelevision.current) 200.dp else 120.dp
