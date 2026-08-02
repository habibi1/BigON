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
}
