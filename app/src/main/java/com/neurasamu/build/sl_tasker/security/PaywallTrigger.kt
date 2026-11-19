package com.neurasamu.build.sl_tasker.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global trigger for Paywall display. UI observes this.
 * Any limit gate calls show(reason) — MainActivity renders PaywallScreen overlay.
 */
object PaywallTrigger {
    private val _reason = MutableStateFlow<String?>(null)
    val reason: StateFlow<String?> = _reason

    fun show(text: String) { _reason.value = text }
    fun dismiss() { _reason.value = null }
}
