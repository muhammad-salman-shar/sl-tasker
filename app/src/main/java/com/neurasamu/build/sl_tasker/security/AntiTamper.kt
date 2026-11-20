package com.neurasamu.build.sl_tasker.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File
import java.security.MessageDigest

/**
 * Runtime integrity & anti-tamper checks.
 * Detects active debuggers, debuggable repackaging, package cloning,
 * root/hooking/patching tools, and signing certificate mismatches.
 */
object AntiTamper {

    private const val EXPECTED_PACKAGE = "com.neurasamu.build.sl_tasker"

    // Leave blank while CI signs with ephemeral debug keystore.
    // Set to your permanent Release Keystore SHA-256 (uppercase hex, no colons) before Play/Public release.
    private const val EXPECTED_SIGNING_SHA256 = ""

    private val BLOCKED_TOOLS = arrayOf(
        "com.chelpus.lackypatch",
        "com.dimonvideo.luckypatcher",
        "com.forpda.lp",
        "com.android.vending.billing.InAppBillingService.LUCK",
        "com.android.vending.billing.InAppBillingService.COIN",
        "de.robv.android.xposed.installer",
        "org.lsposed.manager"
    )

    private val SU_PATHS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/su/bin/su"
    )

    @Volatile
    private var cachedTampered: Boolean? = null

    fun isTampered(context: Context): Boolean {
        // Always check active debugger live
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            cachedTampered = true
            return true
        }
        cachedTampered?.let { return it }

        val compromised = if (com.neurasamu.build.sl_tasker.BuildConfig.IS_PLAY_BUILD) {
            isDebuggableBuild(context) || isPackageCloned(context)
        } else {
            isDebuggableBuild(context) ||
                isPackageCloned(context) ||
                isSignatureMismatch(context) ||
                isDeviceRooted() ||
                hasPatchingTools(context)
        }

        cachedTampered = compromised
        return compromised
    }

    private fun isDebuggableBuild(context: Context): Boolean {
        return try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun isPackageCloned(context: Context): Boolean {
        return context.packageName != EXPECTED_PACKAGE
    }

    private fun isDeviceRooted(): Boolean {
        if (Build.TAGS?.contains("test-keys") == true) return true
        return SU_PATHS.any { path ->
            try { File(path).exists() } catch (_: Throwable) { false }
        }
    }

    private fun hasPatchingTools(context: Context): Boolean {
        val pm = context.packageManager
        return BLOCKED_TOOLS.any { pkg ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun getCurrentSigningSha256(context: Context): String? {
        return try {
            val pm = context.packageManager
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signers = info.signingInfo?.apkContentsSigners
                signers?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }
            certBytes?.let { bytes ->
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { "%02X".format(it) }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isSignatureMismatch(context: Context): Boolean {
        if (EXPECTED_SIGNING_SHA256.isBlank()) return false
        val current = getCurrentSigningSha256(context) ?: return true
        return !current.equals(EXPECTED_SIGNING_SHA256, ignoreCase = true)
    }
}
