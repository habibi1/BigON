package com.bigon.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Accessor for the Sinema token system. Components and features read every design
 * value through this object — never raw colors, sizes, or styles.
 *
 * This shares its name with the [BigonTheme] composable below, deliberately and
 * legally: Kotlin resolves a classifier and a function in separate namespaces, so
 * `BigonTheme { }` wraps a subtree while `BigonTheme.colors` reads a token.
 * `MaterialTheme` is defined exactly this way, which is the point — the design
 * system should read like the framework it sits on rather than inventing its own
 * shape.
 */
object BigonTheme {
    val colors: BigonColors
        @Composable @ReadOnlyComposable get() = LocalSinemaColors.current
    val typography: BigonTypography
        @Composable @ReadOnlyComposable get() = LocalSinemaTypography.current
    val spacing: BigonSpacing
        @Composable @ReadOnlyComposable get() = LocalSinemaSpacing.current
    val shapes: BigonShapes
        @Composable @ReadOnlyComposable get() = LocalSinemaShapes.current
}

/**
 * App theme. Provides the Sinema tokens and mirrors them into [MaterialTheme] so
 * any Material 3 component used inside features inherits the same palette.
 * Pure commonMain — compiles unchanged for Android and iOS.
 */
@Composable
fun BigonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Defaults to what the configuration reports; a caller may override it to
     * render a television layout in a preview or a test.
     */
    isTelevision: Boolean = isTelevisionConfiguration(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) BigonDarkColors else BigonLightColors
    CompositionLocalProvider(
        LocalSinemaColors provides colors,
        // The palette does not change on a television — the contrast ratios were
        // measured on device and hold at any distance. Only the type does.
        LocalSinemaTypography provides
            if (isTelevision) televisionTypography() else BigonTypography(),
        LocalSinemaSpacing provides BigonSpacing(),
        LocalSinemaShapes provides BigonShapes(),
        LocalBigonIsTelevision provides isTelevision,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            content = content,
        )
    }
}

private fun BigonColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        outline = outline,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
    )
}
