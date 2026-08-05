package com.bigon.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStorePreferenceStorage(
    private val dataStore: DataStore<Preferences>,
) : PreferenceStorage {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REGION = stringPreferencesKey("content_region")
    }

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[Keys.ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.ONBOARDING_COMPLETED] = value }
    }

    override val themeMode: Flow<String?> =
        dataStore.data.map { preferences -> preferences[Keys.THEME_MODE] }

    override suspend fun setThemeMode(value: String) {
        dataStore.edit { preferences -> preferences[Keys.THEME_MODE] = value }
    }

    override val region: Flow<String?> =
        dataStore.data.map { preferences -> preferences[Keys.REGION] }

    override suspend fun setRegion(value: String?) {
        dataStore.edit { preferences ->
            // Removing rather than storing a sentinel: absence is what "follow
            // the device" means, and it is what a fresh install already has.
            if (value == null) preferences.remove(Keys.REGION) else preferences[Keys.REGION] = value
        }
    }
}
