package com.neurasamu.build.sl_tasker.domain.engine

object RecoveryEngine {
    const val TARGET_EP = 30
    const val HP_RESTORE = 50
    const val MAX_HP = 100

    fun shouldTrigger(health: Int): Boolean = health < 50 && health > 0
}
