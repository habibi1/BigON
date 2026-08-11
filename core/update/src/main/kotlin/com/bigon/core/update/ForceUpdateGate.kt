package com.bigon.core.update

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Wraps an app in a force update.
 *
 * This is the whole integration. Put it directly inside the host's theme and
 * pass the app as [content]:
 *
 * ```
 * AppTheme {
 *     ForceUpdateGate {
 *         MyApp()
 *     }
 * }
 * ```
 *
 * Everything else — asking Play on every resume, resuming an interrupted
 * update, registering the IntentSender launcher, blocking back, drawing the
 * screen — happens in here. There is deliberately nothing for the host
 * Activity to hold, register, or remember to call, because every one of those
 * is a chance for an app to integrate this *almost* correctly and ship a build
 * that cannot be recalled.
 *
 * The default screen is drawn from [MaterialTheme], so it inherits whatever
 * the host app's theme provides. Apps wanting their own can pass [blockedScreen].
 *
 * @param config thresholds for what counts as urgent — see [UpdateConfig].
 * @param blockedScreen replaces the default blocking screen; it is handed a
 *   callback that starts Play's flow.
 */
@Composable
fun ForceUpdateGate(
    modifier: Modifier = Modifier,
    config: UpdateConfig = UpdateConfig(),
    blockedScreen: @Composable (ForceUpdateState, onUpdate: () -> Unit) -> Unit = { state, onUpdate ->
        DefaultBlockedScreen(state = state, onUpdate = onUpdate)
    },
    content: @Composable () -> Unit,
) {
    val viewModel: ForceUpdateViewModel =
        hiltViewModel<ForceUpdateViewModel, ForceUpdateViewModel.Factory>(
            key = "force-update",
        ) { factory -> factory.create(config) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Registered here rather than in the host Activity: an ActivityResult
    // launcher must exist before it is used, and making that the app's job is
    // exactly the kind of setup step that gets forgotten.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { viewModel.onPlayFlowResult() }

    // Re-checked on every resume: a release can be promoted to critical while
    // the app sits in the background, and Play requires an interrupted
    // immediate update to be resumed at this exact moment.
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            viewModel.refresh()
            // Straight to Play the first time, so a required update is one tap
            // rather than two. Backing out lands on the gate instead.
            if (viewModel.consumeAutoStart()) viewModel.startUpdate(launcher)
        }
        onPauseOrDispose { job.cancel() }
    }

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
            BackHandler(enabled = true) {}
            blockedScreen(state) { viewModel.startUpdate(launcher) }
        } else {
            content()
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
    state: ForceUpdateState,
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
            modifier = Modifier.fillMaxWidth(),
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
