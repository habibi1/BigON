package com.bigon.sinema.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.datastore.PreferenceStorage
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.MovieRepository
import com.bigon.core.common.AppResult
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
    private val movieRepository: MovieRepository,
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

        preferences.region
            .onEach { chosen -> _state.update { it.copy(chosenRegion = chosen) } }
            .launchIn(viewModelScope)

        refreshCacheSize()
        refreshActiveRegion()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ThemeChanged -> viewModelScope.launch {
                preferences.setThemeMode(intent.mode.storageValue)
            }
            SettingsIntent.RegionPickerOpened -> {
                _state.update { it.copy(isRegionPickerOpen = true, regionQuery = "") }
                loadRegions()
            }
            SettingsIntent.RegionPickerDismissed ->
                _state.update { it.copy(isRegionPickerOpen = false, regionQuery = "") }
            is SettingsIntent.RegionQueryChanged ->
                _state.update { it.copy(regionQuery = intent.query) }
            is SettingsIntent.RegionSelected -> viewModelScope.launch {
                _state.update { it.copy(isRegionPickerOpen = false, regionQuery = "") }
                // The repository drops region-scoped caches as part of this, so
                // the next screen the user opens refetches rather than showing
                // one region's answers under another's label.
                movieRepository.setRegion(intent.code)
                refreshActiveRegion()
                refreshCacheSize()
            }
            SettingsIntent.ClearCache -> viewModelScope.launch {
                _state.update { it.copy(isClearingCache = true) }
                cacheOperator.clear()
                _state.update { it.copy(isClearingCache = false) }
                refreshCacheSize()
            }
        }
    }

    private fun refreshActiveRegion() {
        viewModelScope.launch {
            _state.update { it.copy(activeRegion = movieRepository.activeRegion()) }
        }
    }

    /**
     * Fetched when the picker opens rather than at startup: it is one request
     * that most sessions never need, and it is only ever read here.
     */
    private fun loadRegions() {
        if (_state.value.regions.isNotEmpty() || _state.value.isLoadingRegions) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRegions = true, regionError = null) }
            when (val result = movieRepository.availableRegions()) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoadingRegions = false, regions = result.value)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoadingRegions = false, regionError = result.error.toUiText())
                }
            }
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            _state.update { it.copy(cacheBytes = cacheOperator.sizeBytes()) }
        }
    }
}
