package com.bigon.sinema.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.datastore.PreferenceStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ThemeMode(val storageValue: String) {
    System("system"),
    Dark("dark"),
    Light("light"),
    ;

    companion object {
        /** The theme a fresh install opens in, before anyone has chosen. */
        val Default: ThemeMode = Dark

        /**
         * Null is "never chosen", which is the case on a fresh install — and
         * an unrecognised value means storage written by a build that knew a
         * mode this one does not. Both resolve to [Default] rather than to
         * [System]: the app is a poster grid, its splash is a fixed dark field
         * that does not follow system night mode, and following the system from
         * there would open a light app behind a dark launch on half of all
         * devices. Choosing [System] explicitly still works and still follows.
         */
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

/**
 * The single source of truth for the app theme, read at the shell level so
 * every screen recolours together. Settings writes; this only observes.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferences: PreferenceStorage,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .map(ThemeMode::fromStorage)
        // Seeded with the same default the storage layer resolves to, so the
        // first frame cannot be a different theme from the second.
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Default)
}
