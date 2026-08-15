package com.bigon.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotations for the Sinema design system.
 *
 * Annotating a preview function with one of these renders it once per variant,
 * so a single `@BigonThemePreview fun` proves a component in both themes without
 * duplicated preview code. Combine them freely — annotations multiply, so
 * `@BigonThemePreview @BigonFontScalePreview` yields 2 × 4 renders.
 *
 * Background colours below are the raw `BigonColors` grounds (Night900 / the
 * light page tone), so the preview canvas matches what the app actually draws.
 */

private const val DARK_GROUND = 0xFF101418
private const val LIGHT_GROUND = 0xFFFAF9F7

/**
 * Single render on the app's primary (dark) ground. Used for the individual
 * state previews that sit beside each component, so a component file with a
 * dozen states stays fast to render in the IDE split view.
 */
@Preview(
    showBackground = true,
    backgroundColor = DARK_GROUND,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class BigonPreview

/** Light + dark. Used for each component's canonical preview. */
@Preview(
    name = "Light",
    group = "theme",
    showBackground = true,
    backgroundColor = LIGHT_GROUND,
)
@Preview(
    name = "Dark",
    group = "theme",
    showBackground = true,
    backgroundColor = DARK_GROUND,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class BigonThemePreview

/**
 * Accessibility sweep. 200% is the Android maximum users can set; anything that
 * clips or truncates here is a real defect, not a preview artifact.
 */
@Preview(name = "Font 85%", group = "font scale", fontScale = 0.85f, showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Font 100%", group = "font scale", fontScale = 1.0f, showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Font 150%", group = "font scale", fontScale = 1.5f, showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Font 200%", group = "font scale", fontScale = 2.0f, showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class BigonFontScalePreview

/**
 * The three window-size classes the app must support (§5.3). Used on screens
 * and on `BigonAppScaffold`, which switches between bottom bar and rail at 600dp.
 */
@Preview(name = "Phone · compact", group = "device", device = "spec:width=411dp,height=891dp", showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Foldable · medium", group = "device", device = "spec:width=673dp,height=841dp", showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet · expanded", group = "device", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class BigonDevicePreview

/** Right-to-left layout check for anything with directional padding or icons. */
@Preview(name = "LTR", group = "direction", showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "RTL", group = "direction", showBackground = true, backgroundColor = DARK_GROUND, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "ar")
annotation class BigonRtlPreview
