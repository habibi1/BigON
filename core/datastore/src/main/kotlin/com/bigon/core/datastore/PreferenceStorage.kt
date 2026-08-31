package com.bigon.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Portable port over key-value storage (§4.5): typed getters returning [Flow],
 * suspend setters. Consumers never see DataStore types.
 */
interface PreferenceStorage {
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(value: Boolean)

    /**
     * Persisted theme selection. Stored as a plain string ("system"/"dark"/
     * "light") so this module never depends on presentation enums; null means
     * the user has never chosen. What that resolves to is a presentation
     * decision and lives in ThemeMode.fromStorage, not here — this layer only
     * reports the absence.
     */
    val themeMode: Flow<String?>
    suspend fun setThemeMode(value: String)

    /**
     * Chosen content region as an ISO-3166-1 alpha-2 code, or null to follow
     * the device locale.
     *
     * Null is meaningfully different from a stored value that happens to match
     * the device: "follow my phone" keeps working when the user travels or
     * changes their system setting, whereas an explicit "US" stays US.
     */
    val region: Flow<String?>
    suspend fun setRegion(value: String?)
}
