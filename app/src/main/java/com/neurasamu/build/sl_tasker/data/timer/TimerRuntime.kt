package com.neurasamu.build.sl_tasker.data.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TimerRuntime {
    private val _active = MutableStateFlow<ActiveTimer?>(null)
    val active: StateFlow<ActiveTimer?> = _active

    fun set(timer: ActiveTimer?) { _active.value = timer }
}
