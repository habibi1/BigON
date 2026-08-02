package com.bigon.sinema.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.datastore.PreferenceStorage
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.sinema.di.CacheOperator
import com.bigon.sinema.ui.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferenceStorage,
    private val cacheOperator: CacheOperator,
    tracker: AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        tracker.track(AnalyticsEvent.ScreenView("settings"))

        preferences.themeMode
            .map(ThemeMode::fromStorage)
            .onEach { mode -> _state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)

        refreshCacheSize()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ThemeChanged -> viewModelScope.launch {
                preferences.setThemeMode(intent.mode.storageValue)
            }
            SettingsIntent.ClearCache -> viewModelScope.launch {
                _state.update { it.copy(isClearingCache = true) }
                cacheOperator.clear()
                _state.update { it.copy(isClearingCache = false) }
                refreshCacheSize()
            }
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            _state.update { it.copy(cacheBytes = cacheOperator.sizeBytes()) }
        }
    }
}
