package com.bigon.core.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the gate should draw while an update is being insisted upon. */
data class ForceUpdateState(
    val isBlocking: Boolean = false,
    val reason: ForceReason? = null,
    val versionCode: Int = 0,
    /**
     * True while Play's own full-screen flow is up. The gate stays opaque
     * underneath — Play simply covers it — so backing out of Play's UI reveals
     * the block again rather than the app.
     */
    val isPlayFlowVisible: Boolean = false,
)

/**
 * Holds the force-update decision for as long as the host Activity lives.
 *
 * The policy arrives as an assisted parameter rather than a binding so that two
 * apps sharing this module can disagree about what counts as urgent without
 * either of them editing it — and without the DI graph having to arbitrate.
 */
@HiltViewModel(assistedFactory = ForceUpdateViewModel.Factory::class)
class ForceUpdateViewModel @AssistedInject constructor(
    private val source: UpdateSource,
    @Assisted config: UpdateConfig,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(config: UpdateConfig): ForceUpdateViewModel
    }

    private val policy = UpdatePolicy(config)

    private val _state = MutableStateFlow(ForceUpdateState())
    val state: StateFlow<ForceUpdateState> = _state.asStateFlow()

    /**
     * Guards the one genuinely dangerous shape here: resume fires the check,
     * the check starts Play's flow, backing out of Play fires resume again.
     * That loop would pin the user between two screens with no way to read
     * either. So the hand-off happens once per time the gate goes up; after
     * that it is the button's job.
     */
    private var autoStartPending = false

    /**
     * Re-asks Play and re-applies the policy. Called on every resume, because a
     * release can be promoted to critical while the app sits in the background,
     * and because Play requires an interrupted immediate update to be resumed
     * at exactly this moment.
     */
    suspend fun refresh() {
        when (val decision = policy.decide(source.check())) {
            // Never latch: a build that stops being blocked — policy changed,
            // Play withdrew the update — gets its app back.
            is UpdateDecision.Continue -> {
                _state.value = ForceUpdateState()
                autoStartPending = false
            }

            is UpdateDecision.Force -> {
                if (!_state.value.isBlocking) autoStartPending = true
                _state.update {
                    it.copy(
                        isBlocking = true,
                        reason = decision.reason,
                        versionCode = decision.versionCode,
                    )
                }
            }
        }
    }

    /**
     * True once each time the gate goes up, so the caller can hand straight off
     * to Play instead of making the user press a button to be told they have no
     * choice. Returns false ever after — see [autoStartPending].
     */
    fun consumeAutoStart(): Boolean = autoStartPending.also { autoStartPending = false }

    /** Hands off to Play's immediate flow. */
    fun startUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val started = source.startImmediateFlow(launcher)
        _state.update { it.copy(isPlayFlowVisible = started) }
    }

    /**
     * Play's flow closed. Cancellation is the interesting case: the user pressed
     * back on a mandatory update, so the gate must still be there when they land
     * back in the app. That is the difference between this and a flexible
     * update — there is no "not now".
     */
    fun onPlayFlowResult() {
        _state.update { it.copy(isPlayFlowVisible = false) }
    }
}
