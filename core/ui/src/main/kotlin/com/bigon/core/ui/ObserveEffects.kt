package com.bigon.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot ViewModel effects (§5.2) from a screen. Kept CMP-portable:
 * plain LaunchedEffect, no lifecycle-Android APIs.
 */
@Composable
fun <T> ObserveEffects(flow: Flow<T>, onEffect: suspend (T) -> Unit) {
    LaunchedEffect(flow) {
        flow.collect(onEffect)
    }
}
