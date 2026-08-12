package com.bigon.core.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Rule
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

/**
 * The gate has exactly one job, and it is a UI fact: a blocked build must not
 * be reachable.
 *
 * These exist because the first version got it wrong in a way no unit test
 * could see. It drew an opaque screen *over* the app, which is indistinguishable
 * in a screenshot — but a Compose surface does not consume pointer input unless
 * something asks it to, so taps went straight through to the app underneath and
 * it stayed in the accessibility tree. Drawing over and replacing look the same
 * until something tries to touch what is behind, so that is what these do.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateGateTest {

    @get:Rule
    val compose = createComposeRule()

    private val blocked = UpdateUiState(isBlocking = true, reason = ForceReason.Priority)

    private fun setGate(
        state: UpdateUiState,
        onAppClick: () -> Unit = {},
        onUpdate: () -> Unit = {},
        onAccept: () -> Unit = {},
        onDismiss: () -> Unit = {},
        onRestart: () -> Unit = {},
    ) {
        compose.setContent {
            UpdateGateContent(
                state = state,
                onUpdate = onUpdate,
                onAccept = onAccept,
                onDismiss = onDismiss,
                onRestart = onRestart,
                optionalPrompt = { _, accept, dismiss, restart ->
                    Column {
                        Text(text = PROMPT)
                        Text(text = ACCEPT, modifier = Modifier.clickable { accept() })
                        Text(text = DISMISS, modifier = Modifier.clickable { dismiss() })
                        Text(text = RESTART, modifier = Modifier.clickable { restart() })
                    }
                },
                // Shaped like a real blocking screen: the surface itself is
                // inert and only the button is clickable. An all-clickable
                // stand-in would swallow every tap and make the pass-through
                // test pass against code that still leaks.
                blockedScreen = { _, update ->
                    Box(Modifier.fillMaxSize()) {
                        Text(text = BLOCKED)
                        Text(
                            text = UPDATE,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .clickable { update() },
                        )
                    }
                },
            ) {
                Box(Modifier.fillMaxSize().clickable { onAppClick() }) {
                    Text(text = APP)
                }
            }
        }
    }

    @Test
    fun the_app_shows_when_nothing_is_blocking() {
        setGate(UpdateUiState(isBlocking = false))

        compose.onNodeWithText(APP).assertIsDisplayed()
        compose.onNodeWithText(BLOCKED).assertDoesNotExist()
    }

    @Test
    fun the_blocked_screen_replaces_the_app_rather_than_covering_it() {
        setGate(blocked)

        compose.onNodeWithText(BLOCKED).assertIsDisplayed()
        // Absent, not merely hidden: anything left in the tree can still be
        // reached by touch exploration when it cannot be seen.
        compose.onNodeWithText(APP).assertDoesNotExist()
    }

    @Test
    fun the_app_cannot_be_clicked_through_the_gate() {
        var appClicks = 0
        setGate(blocked, onAppClick = { appClicks++ })

        // Deliberately a raw tap on the root rather than performClick on a
        // node: clicking a node by semantics dispatches straight to it and
        // would pass even when the app is still underneath. Only a real
        // coordinate goes through hit-testing, which is what the bug was.
        compose.onRoot().performTouchInput { click(center) }

        assertEquals("the app received a tap through the gate", 0, appClicks)
    }

    @Test
    fun the_update_action_reaches_the_caller() {
        var updates = 0
        setGate(blocked, onUpdate = { updates++ })

        compose.onNodeWithText(UPDATE).performClick()

        assertEquals("expected exactly one update request", 1, updates)
    }


    @Test
    fun an_optional_update_leaves_the_app_usable() {
        // The whole difference from a forced update: the prompt is over a
        // working app, not instead of one.
        var appClicks = 0
        setGate(
            UpdateUiState(isBlocking = false, showOptionalPrompt = true, versionCode = 9),
            onAppClick = { appClicks++ },
        )

        compose.onNodeWithText(PROMPT).assertIsDisplayed()
        compose.onNodeWithText(APP).assertIsDisplayed()

        compose.onNodeWithText(APP).performClick()
        assertEquals(1, appClicks, "the app was unreachable behind an optional prompt")
    }

    @Test
    fun a_forced_update_outranks_an_optional_one() {
        // Both flags set is not a state the ViewModel produces, but the gate
        // should not depend on that: blocking wins, and the sheet is nowhere.
        setGate(UpdateUiState(isBlocking = true, showOptionalPrompt = true))

        compose.onNodeWithText(BLOCKED).assertIsDisplayed()
        compose.onNodeWithText(PROMPT).assertDoesNotExist()
        compose.onNodeWithText(APP).assertDoesNotExist()
    }

    @Test
    fun a_finished_download_offers_a_restart_without_being_asked_again() {
        // The prompt flag is false — the user already accepted. What brings the
        // sheet back is the download completing.
        var restarts = 0
        setGate(
            UpdateUiState(isBlocking = false, showOptionalPrompt = false, install = InstallProgress.Downloaded),
            onRestart = { restarts++ },
        )

        compose.onNodeWithText(PROMPT).assertIsDisplayed()
        compose.onNodeWithText(RESTART).performClick()
        assertEquals(1, restarts)
    }

    @Test
    fun a_download_in_flight_does_not_reopen_the_prompt() {
        setGate(
            UpdateUiState(
                isBlocking = false,
                showOptionalPrompt = false,
                install = InstallProgress.Downloading(bytesDownloaded = 1, totalBytes = 2),
            ),
        )

        compose.onNodeWithText(PROMPT).assertDoesNotExist()
        compose.onNodeWithText(APP).assertIsDisplayed()
    }

    private companion object {
        const val APP = "APP-CONTENT"
        const val BLOCKED = "BLOCKED-SCREEN"
        const val UPDATE = "UPDATE-ACTION"
        const val PROMPT = "OPTIONAL-PROMPT"
        const val ACCEPT = "OPTIONAL-ACCEPT"
        const val DISMISS = "OPTIONAL-DISMISS"
        const val RESTART = "OPTIONAL-RESTART"
    }
}
