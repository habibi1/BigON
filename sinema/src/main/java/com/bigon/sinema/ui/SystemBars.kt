package com.bigon.sinema.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Keeps the status- and navigation-bar icons legible against the app's own
 * background.
 *
 * `enableEdgeToEdge()` decides icon colour from the *system* night mode, but the
 * app's theme is user-chosen and persisted — so picking Dark while the system is
 * Light left black icons on our near-black background, invisible. Driving the
 * appearance from the resolved theme instead fixes both mismatches.
 */
@Composable
fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    LaunchedEffect(darkTheme, view) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            // "Light bars" means a light *background*, i.e. dark icons.
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
