package com.bigon.tmdb.data.movie

import com.bigon.core.datastore.PreferenceStorage
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the user is and what language they read.
 *
 * The two are separate on purpose: TMDB keys availability and certification by
 * *country* and localised copy by *language*, and they disagree constantly — an
 * English-reading user in Indonesia wants Indonesian streaming services with
 * English copy, which a single "locale" string cannot express.
 *
 * An interface rather than direct calls into storage so the fallback chains
 * stay testable without a DataStore or a mutated global JVM locale.
 */
interface RegionProvider {
    /**
     * ISO-3166-1 alpha-2, uppercase. Suspending because the answer is a stored
     * preference, not a device property — every caller is already inside a
     * suspend function, and caching it in a field would make a region change
     * take effect only after a process restart.
     */
    suspend fun region(): String

    /** ISO-639-1, lowercase. Not user-selectable; the device knows this one. */
    fun language(): String
}

/**
 * Prefers the region chosen in Settings, falling back to the device locale.
 *
 * The fallback is deliberately not resolved once at construction: a user who
 * has never chosen should keep following their phone, including after they
 * change it or travel.
 */
@Singleton
class LocaleRegionProvider @Inject constructor(
    private val preferences: PreferenceStorage,
) : RegionProvider {

    override suspend fun region(): String =
        preferences.region.first()?.takeIf { it.length == 2 }?.uppercase() ?: deviceRegion()

    override fun language(): String =
        Locale.getDefault().language.takeIf { it.isNotBlank() }?.lowercase()
            ?: MovieMapper.FALLBACK_LANGUAGE

    private fun deviceRegion(): String =
        Locale.getDefault().country.takeIf { it.length == 2 }?.uppercase()
            ?: MovieMapper.FALLBACK_REGION
}
