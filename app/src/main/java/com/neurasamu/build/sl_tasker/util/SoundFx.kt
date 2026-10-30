package com.neurasamu.build.sl_tasker.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundFx {
    private var tone: ToneGenerator? = null
    var muted: Boolean = false

    fun setup() {
        if (tone != null) return
        try {
            tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Throwable) {
            tone = null
        }
    }

    fun complete() {
        if (muted) return
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (_: Throwable) {}
    }

    fun levelUp() {
        if (muted) return
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_PROMPT, 400)
        } catch (_: Throwable) {}
    }

    fun penalty() {
        if (muted) return
        try {
            tone?.startTone(ToneGenerator.TONE_SUP_ERROR, 400)
        } catch (_: Throwable) {}
    }

    fun error() {
        if (muted) return
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (_: Throwable) {}
    }

    fun tap() {
        if (muted) return
        try {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
        } catch (_: Throwable) {}
    }

    fun giveUp() {
        if (muted) return
        try {
            tone?.startTone(ToneGenerator.TONE_SUP_RADIO_NOTAVAIL, 300)
        } catch (_: Throwable) {}
    }
}
