package com.neurasamu.build.sl_tasker.security

import android.app.Application

object BillingBootstrap {
    fun init(app: Application) {
        PlayBillingManager.init(app)
    }
}
