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
     * the user has never chosen and the app should follow the system.
     */
    val themeMode: Flow<String?>
    suspend fun setThemeMode(value: String)
}
