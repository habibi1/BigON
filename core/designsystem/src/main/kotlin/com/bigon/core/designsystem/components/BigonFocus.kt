package com.bigon.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * The focus ring — the single thing that decides whether this app is usable
 * with a remote control.
 *
 * `Modifier.clickable` already makes a component focusable, so every control in
 * this design system could always *receive* D-pad focus; none of them showed
 * it. On a touch screen that is invisible and harmless, because a finger points
 * at what it wants. On a television it is the whole interface: the user's only
 * way to know where they are is that one thing looks different.
 *
 * **Order matters.** `onFocusChanged` reports the state of focus modifiers that
 * come *after* it in the chain, so this must be applied before the `clickable`
 * that provides the focusability — not after it:
 *
 * ```kotlin
 * Modifier
 *     .focusRing(shape)          // observes …
 *     .clip(shape)
 *     .background(colour)
 *     .clickable(onClick = …)    // … this
 * ```
 *
 * Drawn unconditionally rather than only on television. A ring that appears
 * only on one form factor is a ring nobody tests, and hardware keyboards,
 * Chromebooks and accessibility switch devices all drive focus on a phone.
 */
@Composable
fun Modifier.focusRing(shape: Shape, width: Dp = 3.dp): Modifier {
    var focused by remember { mutableStateOf(false) }
    val ringColor = BigonTheme.colors.primary
    return this
        .onFocusChanged { focused = it.isFocused }
        .then(if (focused) Modifier.border(width, ringColor, shape) else Modifier)
}
