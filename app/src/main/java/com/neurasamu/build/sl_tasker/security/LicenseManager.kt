package com.neurasamu.build.sl_tasker.security

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted license storage bound to the device's ANDROID_ID.
 * Uses Android Keystore (AES-256-GCM) so a decompiled APK cannot read or
 * flip a plain boolean. Any attempt to copy prefs to another device will fail.
 */
object LicenseManager {

    private const val KEYSTORE_ALIAS = "sl_tasker_license_key"
    private const val PREFS_NAME = "sl_tasker_secure_prefs"
    private var appContext: Context? = null
    private const val KEY_PRO = "enc_pro_status"
    private const val KEY_TRIAL_HARD = "enc_trial_hard"
    private const val KEY_TRIAL_CRITICAL = "enc_trial_critical"

    // In-memory cache + reactive flow for instant UI badge updates
    private val _isProFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isProFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isProFlow
    @Volatile private var cachedPro: Boolean? = null
    @Volatile private var cachedTrialHard: Boolean? = null
    @Volatile private var cachedTrialCritical: Boolean? = null

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        if (AntiTamper.isTampered(ctx)) {
            setPro(ctx, false)
        } else {
            _isProFlow.value = isPro(ctx)
        }
    }

    /** Convenience — uses static context. Safe fallback: returns false if not init'd. */
    fun isPro(): Boolean = appContext?.let { isPro(it) } ?: false

    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        return androidId
    }

    /** SHA-256(deviceId + secret) — first 8 chars, uppercase */
    fun generateActivationToken(context: Context): String {
        val raw = getDeviceId(context) + "SL_TASKER_V1"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
            .replace("[^A-Za-z0-9]".toRegex(), "")
            .take(8)
            .uppercase()
    }

    fun verifyActivationCode(context: Context, userInput: String): Boolean {
        if (AntiTamper.isTampered(context)) {
            setPro(context, false)
            return false
        }
        val expected = generateActivationToken(context)
        if (userInput.trim().uppercase() == expected) {
            setPro(context, true)
            return true
        }
        return false
    }

    // ----- Pro Status -----
    fun isPro(context: Context): Boolean {
        if (AntiTamper.isTampered(context)) {
            if (cachedPro == true) setPro(context, false)
            cachedPro = false
            _isProFlow.value = false
            return false
        }
        cachedPro?.let { return it }
        val v = decryptValue(context, KEY_PRO) == "1"
        cachedPro = v
        _isProFlow.value = v
        return v
    }

    fun setPro(context: Context, value: Boolean) {
        val wasPro = cachedPro == true || _isProFlow.value
        cachedPro = value
        _isProFlow.value = value
        encryptValue(context, KEY_PRO, if (value) "1" else "0")
        if (value && !wasPro) {
            try {
                com.neurasamu.build.sl_tasker.ui.theme.ThemeState.selectTheme(
                    com.neurasamu.build.sl_tasker.ui.theme.AppThemeId.ROSE_CREAM
                )
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(
                        "active_theme_id",
                        com.neurasamu.build.sl_tasker.ui.theme.AppThemeId.ROSE_CREAM.name
                    )
                    .apply()
            } catch (_: Throwable) {}
        }
    }

    // ----- Hard trial -----
    fun hasUsedHardTrial(context: Context): Boolean {
        cachedTrialHard?.let { return it }
        val v = decryptValue(context, KEY_TRIAL_HARD) == "1"
        cachedTrialHard = v
        return v
    }

    fun markHardTrialUsed(context: Context) {
        cachedTrialHard = true
        encryptValue(context, KEY_TRIAL_HARD, "1")
    }

    // ----- Critical trial -----
    fun hasUsedCriticalTrial(context: Context): Boolean {
        cachedTrialCritical?.let { return it }
        val v = decryptValue(context, KEY_TRIAL_CRITICAL) == "1"
        cachedTrialCritical = v
        return v
    }

    fun markCriticalTrialUsed(context: Context) {
        cachedTrialCritical = true
        encryptValue(context, KEY_TRIAL_CRITICAL, "1")
    }

    // ----- Internal Keystore helpers -----
    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val kg = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        kg.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    private fun encryptValue(context: Context, key: String, plain: String) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val packed = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(ct, Base64.NO_WRAP)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(key, packed).apply()
        } catch (_: Throwable) {}
    }

    private fun decryptValue(context: Context, key: String): String? {
        return try {
            val packed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, null) ?: return null
            val parts = packed.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ct = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }
}
