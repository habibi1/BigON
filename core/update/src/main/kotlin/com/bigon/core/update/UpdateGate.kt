package com.bigon.core.update

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Wraps an app in Play's in-app updates — both kinds.
 *
 * This is the whole integration. Put it directly inside the host's theme and
 * pass the app as [content]:
 *
 * ```
 * AppTheme {
 *     UpdateGate {
 *         MyApp()
 *     }
 * }
 * ```
 *
 * A **forced** update replaces the app with an unskippable screen. An
 * **optional** one shows a bottom sheet over an app that stays fully usable,
 * downloads in the background, and offers a restart when it is ready.
 *
 * Everything else — asking Play on every resume, resuming an interrupted
 * update, registering the IntentSender launchers, handling back, counting how
 * often someone has said "not now" — happens in here. There is deliberately
 * nothing for the host Activity to hold, register, or remember to call, because
 * every one of those is a chance for an app to integrate this *almost*
 * correctly and ship a build that cannot be recalled.
 *
 * Both default surfaces are drawn from [MaterialTheme], so they inherit
 * whatever the host app's theme provides.
 *
 * @param config thresholds for what counts as urgent, and how often to ask —
 *   see [UpdateConfig].
 * @param blockedScreen replaces the forced-update screen.
 * @param optionalPrompt replaces the optional-update sheet.
 */
@Composable
fun UpdateGate(
    modifier: Modifier = Modifier,
    config: UpdateConfig = UpdateConfig(),
    blockedScreen: @Composable (UpdateUiState, onUpdate: () -> Unit) -> Unit = { state, onUpdate ->
        DefaultBlockedScreen(state = state, onUpdate = onUpdate)
    },
    optionalPrompt: @Composable (
        UpdateUiState,
        onAccept: () -> Unit,
        onDismiss: () -> Unit,
        onRestart: () -> Unit,
    ) -> Unit = { state, onAccept, onDismiss, onRestart ->
        DefaultOptionalPrompt(state, onAccept, onDismiss, onRestart)
    },
    content: @Composable () -> Unit,
) {
    val viewModel: UpdateViewModel =
        hiltViewModel<UpdateViewModel, UpdateViewModel.Factory>(
            key = "app-update",
        ) { factory -> factory.create(config) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Registered here rather than in the host Activity: an ActivityResult
    // launcher must exist before it is used, and making that the app's job is
    // exactly the kind of setup step that gets forgotten.
    val immediateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { viewModel.onPlayFlowResult() }

    // Separate from the immediate launcher because the results mean opposite
    // things. Cancelling a forced update changes nothing — the gate stays.
    // Cancelling Play's flexible confirmation *is* a "not now", and has to be
    // counted as one, or the prompt returns on the next launch having learnt
    // nothing.
    val flexibleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) viewModel.dismissOptional()
    }

    // Re-checked on every resume: a release can be promoted to critical while
    // the app sits in the background, and Play requires an interrupted
    // immediate update to be resumed at this exact moment.
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            viewModel.refresh()
            // Straight to Play the first time, so a required update is one tap
            // rather than two. Backing out lands on the gate instead.
            if (viewModel.consumeAutoStart()) viewModel.startUpdate(immediateLauncher)
        }
        onPauseOrDispose { job.cancel() }
    }

    UpdateGateContent(
        state = state,
        onUpdate = { viewModel.startUpdate(immediateLauncher) },
        onAccept = { viewModel.acceptOptional(flexibleLauncher) },
        onDismiss = viewModel::dismissOptional,
        onRestart = viewModel::completeUpdate,
        blockedScreen = blockedScreen,
        optionalPrompt = optionalPrompt,
        modifier = modifier,
        content = content,
    )
}

/**
 * The gate without its dependencies, so the behaviours that matter — a blocked
 * build cannot be reached, an optional one does not get in the way — are
 * testable without Play, Hilt or an update in flight.
 */
@Composable
internal fun UpdateGateContent(
    state: UpdateUiState,
    onUpdate: () -> Unit,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    blockedScreen: @Composable (UpdateUiState, onUpdate: () -> Unit) -> Unit,
    optionalPrompt: @Composable (
        UpdateUiState,
        onAccept: () -> Unit,
        onDismiss: () -> Unit,
        onRestart: () -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val activity = LocalActivity.current

    Box(modifier = modifier.fillMaxSize()) {
        if (state.isBlocking) {
            // The app is not drawn over — it is not composed at all.
            //
            // Drawing an opaque screen on top looks identical and is not the
            // same thing: a Compose surface does not consume pointer input
            // unless something asks it to, so taps land on whatever is behind
            // it, and the app underneath stays in the accessibility tree for
            // TalkBack to walk. A blocked build would keep navigating and
            // keep making requests, invisibly. Removing it from composition
            // is the only version of this that is actually a block.
            //
            // Back leaves the app rather than doing nothing. Being unable to
            // use an obsolete build is the requirement; being unable to put it
            // down is just a trap, and a back press that silently does nothing
            // reads as a hang.
            BackHandler(enabled = true) { activity?.finish() }
            blockedScreen(state, onUpdate)
        } else {
            // The opposite of the forced case, deliberately: the app is
            // composed, interactive, and on top of nothing. An optional update
            // that interrupts is just a forced update with worse manners.
            content()

            if (state.showOptionalPrompt || state.install == InstallProgress.Downloaded) {
                optionalPrompt(state, onAccept, onDismiss, onRestart)
            }
        }
    }
}

/**
 * The default blocking screen: opaque, unskippable, and styled from the host's
 * [MaterialTheme] so it does not look like a different app.
 *
 * Not a dialog — a dialog implies dismissal.
 */
@Composable
fun DefaultBlockedScreen(
    state: UpdateUiState,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // The app names itself rather than this module naming it, which is what
    // lets the same copy ship in any app.
    val context = LocalContext.current
    val appName = context.applicationInfo.loadLabel(context.packageManager).toString()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            // Hosts run edge-to-edge; without this the copy can slide under a
            // status bar or gesture handle at large font scales.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Capped rather than full-width: on an unfolded foldable or a
            // tablet this is 2000px across, and a sentence spanning all of it
            // is close to unreadable. Narrower than the cap it simply fills.
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDownload,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = "Update required",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = explain(state.reason, appName),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // Opaque even while Play's flow is up. The gate is what stands
            // between a blocked build and the app, so it never becomes a window
            // onto it — Play's UI simply covers this, and if Play's UI goes away
            // the block is still here.
            if (state.isPlayFlowVisible) {
                Text(
                    text = "Opening Google Play…",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                Button(onClick = onUpdate, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Update now")
                }
            }
        }
    }
}

/**
 * The default optional-update surface: a bottom sheet over a working app.
 *
 * A sheet rather than a dialog because a dialog takes the whole screen's
 * attention for something the user is allowed to ignore. It carries two
 * distinct moments — the ask, and the restart once Play has finished
 * downloading — because they are the same conversation and splitting them
 * across two surfaces makes the second one look like a fresh interruption.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultOptionalPrompt(
    state: UpdateUiState,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
    val ready = state.install == InstallProgress.Downloaded
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        // Swiping the sheet away is a "not now" like any other, and is counted
        // as one — otherwise the cheapest way to dismiss is the one the policy
        // never learns about.
        onDismissRequest = { if (!ready) onDismiss() },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (ready) "Update ready" else "Update available",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Text(
                text = if (ready) {
                    "The new version of $appName has downloaded. Restart to finish installing."
                } else {
                    "A newer version of $appName is available. It downloads in the background — " +
                        "you can keep using the app."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            val progress = state.install
            if (progress is InstallProgress.Downloading) {
                progress.fraction?.let { fraction ->
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
            ) {
                if (!ready) {
                    TextButton(onClick = onDismiss) { Text("Not now") }
                }
                Button(onClick = if (ready) onRestart else onAccept) {
                    Text(if (ready) "Restart" else "Update")
                }
            }
        }
    }
}

/**
 * Says why, without pretending to know more than we do. Play reports a version
 * code and a priority, not a changelog, so the copy stays honest about that
 * rather than inventing "critical security fix".
 */
private fun explain(reason: ForceReason?, appName: String): String = when (reason) {
    ForceReason.Resume -> "Your update was interrupted. Finishing it now keeps $appName working."
    ForceReason.Priority, ForceReason.Staleness, null ->
        "This version of $appName is out of date and can no longer be used. " +
            "Install the latest version to continue."
}
