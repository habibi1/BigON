package com.bigon.core.update

import android.util.Log
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Makes [FakeAppUpdateManager] behave like a Play Store that actually does
 * something.
 *
 * The fake records that a flow was started but never progresses on its own: no
 * bytes are downloaded, `DOWNLOADED` never arrives, and the restart prompt
 * therefore cannot be reached. Rehearsing an optional update without this shows
 * only its first frame — the half that was never in doubt.
 *
 * It has to be a state machine rather than a reaction to the fake's own state,
 * because the fake exposes only three queries (`isConfirmationDialogVisible`,
 * `isImmediateFlowVisible`, `isInstallSplashScreenVisible`) and none of them
 * says whether a download is running.
 *
 * Debug-only.
 */
internal fun FakeAppUpdateManager.driveLikePlay(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) = scope.launch {
    var phase = Phase.AwaitingConsent

    while (true) {
        delay(STEP_MILLIS)
        when (phase) {
            Phase.AwaitingConsent -> if (isConfirmationDialogVisible || isImmediateFlowVisible) {
                Log.i(TAG, "user consented — starting download")
                userAcceptsUpdate()
                setTotalBytesToDownload(TOTAL_BYTES)
                setBytesDownloaded(0)
                downloadStarts()
                phase = Phase.Downloading
            }

            Phase.Downloading -> {
                Log.i(TAG, "download complete — awaiting restart")
                setBytesDownloaded(TOTAL_BYTES)
                downloadCompletes()
                phase = Phase.Downloaded
            }

            // completeUpdate() is the app's call, not ours; stopping here is
            // what makes the restart prompt sit and wait like the real thing.
            Phase.Downloaded -> return@launch
        }
    }
}

private enum class Phase { AwaitingConsent, Downloading, Downloaded }

private const val TAG = "ForceUpdate"
private const val STEP_MILLIS = 1_500L
private const val TOTAL_BYTES = 24L * 1024 * 1024
