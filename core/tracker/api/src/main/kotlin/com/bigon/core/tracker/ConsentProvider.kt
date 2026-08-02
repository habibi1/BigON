package com.bigon.core.tracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Consent is a cross-cutting concern handled inside the tracker pipeline (§6.1);
 * call sites never know. A persistent, user-facing consent implementation
 * replaces [StaticConsentProvider] when the privacy flow lands.
 */
interface ConsentProvider {
    val analyticsAllowed: StateFlow<Boolean>
}

class StaticConsentProvider(allowed: Boolean) : ConsentProvider {
    override val analyticsAllowed: StateFlow<Boolean> = MutableStateFlow(allowed)
}
