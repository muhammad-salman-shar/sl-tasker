package com.neurasamu.build.sl_tasker.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object SystemEvents {
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts

    fun toast(message: String) {
        _toasts.tryEmit(message)
    }
}
