package com.bigon.core.update

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers whether the last completed check said this build was blocked.
 *
 * Without it there is a window on every cold start where the app is composed
 * and running while Play is still being asked — measured at roughly 300ms, long
 * enough to fire a request. Closing that window by withholding the UI until the
 * check returns would charge every launch for a case almost nobody hits.
 *
 * So the answer is remembered instead. A build that was blocked last time
 * starts blocked, with no window at all; a build that was fine starts
 * immediately, as before. Only the first launch after a release turns critical
 * still has the gap, and that one is unavoidable without making everyone wait.
 */
interface UpdateGateStore {
    var wasBlocking: Boolean

    /** How often this install has been asked about an optional update. */
    var nagRecord: NagRecord
}

/**
 * SharedPreferences rather than DataStore on purpose: this is a single boolean
 * that has to be readable *synchronously*, before the first frame. An
 * asynchronous read would reintroduce the window it exists to close.
 */
@Singleton
internal class SharedPrefsUpdateGateStore @Inject constructor(
    @ApplicationContext context: Context,
) : UpdateGateStore {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var wasBlocking: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING, false)
        set(value) = prefs.edit { putBoolean(KEY_BLOCKING, value) }

    override var nagRecord: NagRecord
        get() = NagRecord(
            versionCode = prefs.getInt(KEY_NAG_VERSION, 0),
            dismissals = prefs.getInt(KEY_NAG_DISMISSALS, 0),
            launchesSinceDismissal = prefs.getInt(KEY_NAG_LAUNCHES, 0),
        )
        set(value) = prefs.edit {
            putInt(KEY_NAG_VERSION, value.versionCode)
            putInt(KEY_NAG_DISMISSALS, value.dismissals)
            putInt(KEY_NAG_LAUNCHES, value.launchesSinceDismissal)
        }

    private companion object {
        const val FILE = "com.bigon.core.update.gate"
        const val KEY_BLOCKING = "was_blocking"
        const val KEY_NAG_VERSION = "nag_version"
        const val KEY_NAG_DISMISSALS = "nag_dismissals"
        const val KEY_NAG_LAUNCHES = "nag_launches"
    }
}
