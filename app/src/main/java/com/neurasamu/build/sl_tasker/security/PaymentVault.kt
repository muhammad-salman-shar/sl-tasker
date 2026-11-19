package com.neurasamu.build.sl_tasker.security

import android.content.Context
import java.security.MessageDigest

/**
 * Tamper-proof payment configuration vault.
 * Obfuscates payment destination details and verifies SHA-256 integrity
 * of bundled Raast QR assets at runtime to defeat APK repackaging.
 */
object PaymentVault {

    const val ASSET_QR_199 = "pay/qr_199.jpg"
    const val ASSET_QR_1499 = "pay/qr_1499.jpg"

    private val K = intArrayOf(0x5A, 0x3C, 0x7E, 0x19, 0x4B)

    private val ENC_WA = intArrayOf(99, 14, 77, 43, 124, 104, 8, 70, 33, 121, 106, 11)
    private val ENC_ACC = intArrayOf(106, 15, 76, 46, 121, 110, 4, 70, 43, 123, 109)
    private val ENC_TITLE = intArrayOf(23, 105, 54, 88, 6, 23, 125, 58, 57, 24, 27, 112, 51, 88, 5)
    private val ENC_H199 = intArrayOf(107, 120, 79, 45, 13, 111, 11, 60, 33, 120, 98, 126, 75, 33, 10, 28, 8, 75, 90, 13, 24, 10, 78, 43, 14, 25, 4, 56, 92, 121, 110, 14, 58, 46, 122, 109, 12, 74, 91, 114, 108, 4, 61, 45, 10, 111, 12, 74, 41, 122, 31, 5, 76, 41, 10, 30, 12, 79, 43, 122, 110, 120, 77, 42)
    private val ENC_H1499 = intArrayOf(30, 12, 59, 43, 9, 30, 8, 77, 88, 9, 110, 4, 58, 42, 9, 99, 10, 77, 92, 10, 111, 13, 72, 40, 123, 110, 122, 73, 93, 125, 31, 13, 60, 95, 122, 30, 120, 75, 93, 15, 99, 10, 77, 45, 127, 28, 4, 78, 45, 15, 31, 9, 74, 45, 123, 30, 125, 61, 43, 125, 30, 5, 76, 40)

    private fun dec(arr: IntArray): String {
        val sb = StringBuilder(arr.size)
        for (i in arr.indices) {
            sb.append((arr[i] xor K[i % K.size]).toChar())
        }
        return sb.toString()
    }

    fun whatsappNumber(): String = dec(ENC_WA)
    fun localAccount(): String = dec(ENC_ACC)
    fun accountTitle(): String = dec(ENC_TITLE)

    fun verifyAndLoadQrBytes(context: Context, isLifetime: Boolean): ByteArray? {
        if (AntiTamper.isTampered(context)) return null
        val assetPath = if (isLifetime) ASSET_QR_1499 else ASSET_QR_199
        val expectedHash = if (isLifetime) dec(ENC_H1499) else dec(ENC_H199)
        return try {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            val actualHash = MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02X".format(it) }
            if (actualHash.equals(expectedHash, ignoreCase = true)) bytes else null
        } catch (_: Throwable) {
            null
        }
    }
}
