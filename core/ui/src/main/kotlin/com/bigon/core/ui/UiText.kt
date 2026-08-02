package com.bigon.core.ui

import androidx.compose.runtime.Composable

/**
 * Presentation-layer text wrapper so UiState never holds platform string APIs.
 * A `Resource` variant backed by compose.resources is added at CMP migration —
 * call sites already go through [asString] so they won't change.
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
}
