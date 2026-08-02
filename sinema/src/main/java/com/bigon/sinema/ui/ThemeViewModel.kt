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
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: System
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
}
