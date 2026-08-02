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
 * This shares its name with the [SinemaTheme] composable below, deliberately and
 * legally: Kotlin resolves a classifier and a function in separate namespaces, so
 * `SinemaTheme { }` wraps a subtree while `SinemaTheme.colors` reads a token.
 * `MaterialTheme` is defined exactly this way, which is the point — the design
 * system should read like the framework it sits on rather than inventing its own
 * shape.
 */
object SinemaTheme {
    val colors: SinemaColors
        @Composable @ReadOnlyComposable get() = LocalSinemaColors.current
    val typography: SinemaTypography
        @Composable @ReadOnlyComposable get() = LocalSinemaTypography.current
    val spacing: SinemaSpacing
        @Composable @ReadOnlyComposable get() = LocalSinemaSpacing.current
    val shapes: SinemaShapes
        @Composable @ReadOnlyComposable get() = LocalSinemaShapes.current
}

/**
 * App theme. Provides the Sinema tokens and mirrors them into [MaterialTheme] so
 * any Material 3 component used inside features inherits the same palette.
 * Pure commonMain — compiles unchanged for Android and iOS.
 */
@Composable
fun SinemaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) SinemaDarkColors else SinemaLightColors
    CompositionLocalProvider(
        LocalSinemaColors provides colors,
        LocalSinemaTypography provides SinemaTypography(),
        LocalSinemaSpacing provides SinemaSpacing(),
        LocalSinemaShapes provides SinemaShapes(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            content = content,
        )
    }
}

private fun SinemaColors.toMaterialColorScheme(): ColorScheme {
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
