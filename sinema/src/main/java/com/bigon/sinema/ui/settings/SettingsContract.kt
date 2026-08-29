package com.bigon.sinema.ui.settings

import com.bigon.tmdb.model.Region
import com.bigon.core.ui.UiText
import com.bigon.sinema.ui.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val cacheBytes: Long? = null,
    val isClearingCache: Boolean = false,
    /**
     * The region in effect, whether chosen or inherited from the device — the
     * row shows what the app is actually doing, not what was configured.
     */
    val activeRegion: String? = null,
    /** Null while the user has never chosen, which means "follow the device". */
    val chosenRegion: String? = null,
    val regions: List<Region> = emptyList(),
    val isRegionPickerOpen: Boolean = false,
    val isLoadingRegions: Boolean = false,
    val regionError: UiText? = null,
    val regionQuery: String = "",
) {
    /** "128 MB" — or a placeholder while the size is being computed. */
    val cacheLabel: String
        get() = when {
            cacheBytes == null -> "…"
            cacheBytes < 1_048_576L -> "${cacheBytes / 1024} KB"
            else -> "${cacheBytes / 1_048_576L} MB"
        }

    private val activeRegionName: String?
        get() = regions.firstOrNull { it.code == activeRegion }?.name

    /**
     * "Indonesia" when chosen, "Indonesia · device" when inherited. The
     * distinction matters: one keeps tracking the phone, the other does not.
     *
     * The space before "device" is non-breaking, so the qualifier wraps as a
     * unit. Country names run long — "United States of America" — and an
     * ordinary space let the line break after the separator, leaving a "·"
     * dangling at the end of one line and the word alone on the next.
     */
    val regionLabel: String
        get() {
            val name = activeRegionName ?: activeRegion ?: "…"
            return if (chosenRegion == null) "$name ·\u00A0device" else name
        }

    /** Case-insensitive filter over name and code, so "id" finds Indonesia. */
    val visibleRegions: List<Region>
        get() = if (regionQuery.isBlank()) {
            regions
        } else {
            regions.filter {
                it.name.contains(regionQuery, ignoreCase = true) ||
                    it.code.equals(regionQuery.trim(), ignoreCase = true)
            }
        }
}

sealed interface SettingsIntent {
    data class ThemeChanged(val mode: ThemeMode) : SettingsIntent
    data object ClearCache : SettingsIntent
    data object RegionPickerOpened : SettingsIntent
    data object RegionPickerDismissed : SettingsIntent
    data class RegionQueryChanged(val query: String) : SettingsIntent
    /** Null selects "follow the device". */
    data class RegionSelected(val code: String?) : SettingsIntent
}
