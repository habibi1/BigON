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
 * Accessor for the Cine token system. Components and features read every design
 * value through this object — never raw colors, sizes, or styles.
 */
object CineTheme {
    val colors: CineColors
        @Composable @ReadOnlyComposable get() = LocalCineColors.current
    val typography: CineTypography
        @Composable @ReadOnlyComposable get() = LocalCineTypography.current
    val spacing: CineSpacing
        @Composable @ReadOnlyComposable get() = LocalCineSpacing.current
    val shapes: CineShapes
        @Composable @ReadOnlyComposable get() = LocalCineShapes.current
}

/**
 * App theme. Provides the Cine tokens and mirrors them into [MaterialTheme] so
 * any Material 3 component used inside features inherits the same palette.
 * Pure commonMain — compiles unchanged for Android and iOS.
 */
@Composable
fun SinemaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) CineDarkColors else CineLightColors
    CompositionLocalProvider(
        LocalCineColors provides colors,
        LocalCineTypography provides CineTypography(),
        LocalCineSpacing provides CineSpacing(),
        LocalCineShapes provides CineShapes(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            content = content,
        )
    }
}

private fun CineColors.toMaterialColorScheme(): ColorScheme {
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
