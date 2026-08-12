package com.bigon.core.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.ktx.requestAppUpdateInfo
import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.play.core.install.model.UpdateAvailability as PlayAvailability

/**
 * What the controller needs from Play, narrow enough to fake in a unit test.
 * The controller owns the one piece of logic here worth testing on its own —
 * when to hand off, and when handing off again would trap the user in a loop —
 * and that should not require a Play Store to exercise.
 */
interface UpdateSource {
    suspend fun check(): UpdateStatus

    fun startImmediateFlow(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /**
     * Starts Play's flexible flow: the download runs in the background and the
     * app stays usable. Unlike the immediate flow, this one is not finished by
     * Play — [completeUpdate] has to be called once [installState] reports
     * [InstallProgress.Downloaded].
     */
    fun startFlexibleFlow(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /** Download progress for a flexible update in flight. */
    val installState: Flow<InstallProgress>

    /** Restarts the app into the downloaded update. */
    fun completeUpdate()
}

/** Where a flexible download has got to. */
sealed interface InstallProgress {
    data object Idle : InstallProgress

    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : InstallProgress {
        /** 0f..1f, or null while Play has not reported a total yet. */
        val fraction: Float?
            get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else null
    }

    /** Downloaded and waiting for the restart only the app can trigger. */
    data object Downloaded : InstallProgress

    data object Failed : InstallProgress
}

/**
 * The boundary between Play's in-app update API and the rest of the app.
 *
 * Play hands back an [AppUpdateInfo] that is not just data: it is the token the
 * update flow must be started with, and Play rejects a stale one. So the object
 * itself stays here, behind the door, while the app reasons about the
 * framework-free [UpdateStatus] this maps it to.
 */
@Singleton
internal class PlayUpdateSource @Inject constructor(
    private val manager: AppUpdateManager,
) : UpdateSource {

    private companion object {
        const val TAG = "ForceUpdate"
    }

    /** The info backing the last [check]; required to start the flow Play expects. */
    @Volatile
    private var pending: AppUpdateInfo? = null

    /**
     * Asks Play what it has.
     *
     * Every failure path returns "nothing available" rather than propagating.
     * A force-update check that throws is a check that would either crash the
     * app or, worse, block it — and this app is offline-first by design, so a
     * device with no network, no Play Store, or an out-of-date Play Store has
     * to keep working. The gate fails open, always.
     */
    override suspend fun check(): UpdateStatus = runCatching {
        val info = manager.requestAppUpdateInfo()
        pending = info

        val availability = when (info.updateAvailability()) {
            PlayAvailability.UPDATE_AVAILABLE -> UpdateAvailability.Available
            PlayAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> UpdateAvailability.InProgress
            else -> UpdateAvailability.None
        }

        UpdateStatus(
            availability = availability,
            versionCode = info.availableVersionCode(),
            priority = info.updatePriority(),
            stalenessDays = info.clientVersionStalenessDays(),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
        )
    }.getOrElse { error ->
        // Cancellation is not a failed check. runCatching would otherwise
        // swallow it and report "no update", which on a blocked build means
        // backgrounding the app mid-check lowers the gate — and every resume
        // then reopens the app for as long as the next check takes.
        if (error is CancellationException) throw error

        // InstallException here means no Play Store, a Play Store too old to
        // answer, or an app Play does not recognise — all normal on emulators
        // and sideloads, none of them a reason to stop the user.
        Log.w(TAG, "Update check failed; continuing without one", error)
        pending = null
        UpdateStatus(availability = UpdateAvailability.None)
    }

    /**
     * Starts Play's full-screen immediate flow, which is what actually makes an
     * update mandatory: Play blocks the app, downloads, installs and restarts.
     *
     * @return false when there is nothing to start, so the caller can decide
     *   whether to keep blocking or let the app run.
     */
    override fun startImmediateFlow(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean {
        val info = pending ?: return false
        return runCatching {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
            )
        }.getOrElse { error ->
            Log.w(TAG, "Could not start the immediate update flow", error)
            false
        }
    }

    override fun startFlexibleFlow(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ): Boolean {
        val info = pending ?: return false
        return runCatching {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            )
        }.getOrElse { error ->
            Log.w(TAG, "Could not start the flexible update flow", error)
            false
        }
    }

    /**
     * Play reports progress through a listener, not a stream. callbackFlow
     * bridges the two and — the part that matters — unregisters on cancellation,
     * because a listener held by a Singleton outlives every screen that cared.
     */
    override val installState: Flow<InstallProgress> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            trySend(
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADING -> InstallProgress.Downloading(
                        bytesDownloaded = state.bytesDownloaded(),
                        totalBytes = state.totalBytesToDownload(),
                    )

                    InstallStatus.DOWNLOADED -> InstallProgress.Downloaded
                    InstallStatus.FAILED, InstallStatus.CANCELED -> InstallProgress.Failed
                    else -> InstallProgress.Idle
                },
            )
        }
        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    override fun completeUpdate() {
        runCatching { manager.completeUpdate() }
            .onFailure { Log.w(TAG, "Could not complete the update", it) }
    }
}
