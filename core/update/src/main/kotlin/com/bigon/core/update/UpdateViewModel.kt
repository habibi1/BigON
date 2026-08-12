package com.bigon.core.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the update UI draws from. */
data class UpdateUiState(
    // ── Forced ──────────────────────────────────────────────────────────────
    val isBlocking: Boolean = false,
    val reason: ForceReason? = null,
    val versionCode: Int = 0,
    /**
     * True while Play's own full-screen flow is up. The gate stays opaque
     * underneath — Play simply covers it — so backing out of Play's UI reveals
     * the block again rather than the app.
     */
    val isPlayFlowVisible: Boolean = false,

    // ── Optional ────────────────────────────────────────────────────────────
    /** The prompt is showing. Never true at the same time as [isBlocking]. */
    val showOptionalPrompt: Boolean = false,
    /** Progress of a flexible download the user accepted. */
    val install: InstallProgress = InstallProgress.Idle,
)

/**
 * Holds the update decision for as long as the host Activity lives.
 *
 * Both flows live here because they are the same question answered twice — Play
 * is asked once, and what happens next is either "block" or "mention". Splitting
 * them across two ViewModels would mean two checks and a race over which one
 * gets to be on screen.
 *
 * The config arrives as an assisted parameter rather than a binding so that two
 * apps sharing this module can disagree about what counts as urgent, and how
 * often to ask, without either of them editing it.
 */
@HiltViewModel(assistedFactory = UpdateViewModel.Factory::class)
class UpdateViewModel @AssistedInject constructor(
    private val source: UpdateSource,
    private val store: UpdateGateStore,
    @Assisted config: UpdateConfig,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(config: UpdateConfig): UpdateViewModel
    }

    private val policy = UpdatePolicy(config)
    private val nagPolicy = UpdateNagPolicy(config)

    // Starts from what the last completed check concluded, so a build that was
    // blocked yesterday is blocked before the first frame rather than after the
    // first round trip. Reason is left null: the remembered fact is "blocked",
    // not why, and the copy for an unknown reason is the right one anyway.
    private val _state = MutableStateFlow(UpdateUiState(isBlocking = store.wasBlocking))
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    /**
     * Guards the one genuinely dangerous shape here: resume fires the check,
     * the check starts Play's flow, backing out of Play fires resume again.
     * That loop would pin the user between two screens with no way to read
     * either. So the hand-off happens once per time the gate goes up; after
     * that it is the button's job.
     */
    private var autoStartPending = false

    /**
     * Whether a check has actually completed this process. Distinct from "is
     * blocking", because a gate restored from [UpdateGateStore] is blocking
     * before anything has been decided — and that restored gate still owes the
     * user the automatic hand-off to Play.
     */
    private var decided = false

    /** Asked at most once per process, however many times the app resumes. */
    private var optionalAsked = false

    init {
        // One launch, counted once — not once per resume, or a user who
        // switches apps a lot would exhaust the wait in a single sitting.
        store.nagRecord = nagPolicy.onLaunched(store.nagRecord)

        viewModelScope.launch {
            source.installState.collect { progress ->
                _state.update { it.copy(install = progress) }
            }
        }
    }

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
                _state.update { UpdateUiState(install = it.install) }
                autoStartPending = false
                decided = false
                store.wasBlocking = false
            }

            is UpdateDecision.Force -> {
                // A gate restored from the store has not handed off yet, so the
                // hand-off is owed whenever this is the first *decided* block of
                // the process, not merely the first blocking state.
                if (!decided) autoStartPending = true
                decided = true
                store.wasBlocking = true
                _state.update {
                    it.copy(
                        isBlocking = true,
                        reason = decision.reason,
                        versionCode = decision.versionCode,
                        // A forced update outranks a suggestion that was already
                        // on screen; there is nothing left to decline.
                        showOptionalPrompt = false,
                    )
                }
            }

            is UpdateDecision.Suggest -> {
                store.wasBlocking = false
                val show = !optionalAsked &&
                    // Downloading or downloaded means they already said yes.
                    _state.value.install == InstallProgress.Idle &&
                    nagPolicy.shouldPrompt(store.nagRecord, decision.versionCode)
                if (show) optionalAsked = true
                _state.update {
                    it.copy(
                        isBlocking = false,
                        reason = null,
                        versionCode = decision.versionCode,
                        showOptionalPrompt = show,
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

    // ── Optional update ─────────────────────────────────────────────────────

    /**
     * The user accepted. The prompt closes immediately rather than waiting for
     * Play: the download runs in the background and the app stays usable, which
     * is the entire point of the flexible flow.
     */
    fun acceptOptional(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        _state.update { it.copy(showOptionalPrompt = false) }
        source.startFlexibleFlow(launcher)
    }

    /** "Not now" — recorded, because the next ask depends on how often they have. */
    fun dismissOptional() {
        store.nagRecord = nagPolicy.onDismissed(store.nagRecord, _state.value.versionCode)
        _state.update { it.copy(showOptionalPrompt = false) }
    }

    /**
     * Restarts into the downloaded update. Only the app can do this — Play
     * downloads a flexible update but will not install it, and an app that
     * never calls this leaves the update sitting on disk forever.
     */
    fun completeUpdate() = source.completeUpdate()
}
