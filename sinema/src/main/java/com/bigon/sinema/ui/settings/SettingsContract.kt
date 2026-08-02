package com.bigon.sinema.ui.settings

import com.bigon.sinema.ui.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val cacheBytes: Long? = null,
    val isClearingCache: Boolean = false,
) {
    /** "128 MB" — or a placeholder while the size is being computed. */
    val cacheLabel: String
        get() = when {
            cacheBytes == null -> "…"
            cacheBytes < 1_048_576L -> "${cacheBytes / 1024} KB"
            else -> "${cacheBytes / 1_048_576L} MB"
        }
}

sealed interface SettingsIntent {
    data class ThemeChanged(val mode: ThemeMode) : SettingsIntent
    data object ClearCache : SettingsIntent
}
