package com.neurasamu.build.sl_tasker.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object QrBarcodeHelper {

    fun generateSealPayload(): String {
        val token = UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
        return "SLTASKER-SEAL-$token"
    }

    fun generateQrBitmap(payload: String, sizePx: Int = 640): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    fun generatePrintablePosterBitmap(payload: String, label: String): Bitmap {
        val w = 900
        val h = 1120
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 24f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("SL TASKER · PHYSICAL SEAL", w / 2f, 110f, titlePaint)
        canvas.drawText(label.ifBlank { "Focus & Wake-Up Seal" }, w / 2f, 165f, subPaint)

        val qr = generateQrBitmap(payload, 680)
        canvas.drawBitmap(qr, (w - 680) / 2f, 220f, null)

        canvas.drawText(payload, w / 2f, 960f, codePaint)
        canvas.drawText("Stick on wall, desk, or bathroom mirror", w / 2f, 1020f, subPaint)
        return bmp
    }

    fun printQrSeal(context: Context, payload: String, label: String) {
        try {
            val poster = generatePrintablePosterBitmap(payload, label)
            val printer = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }
            printer.printBitmap("SL_Tasker_Physical_Seal", poster)
        } catch (_: Throwable) {}
    }

    fun shareQrSeal(context: Context, payload: String, label: String) {
        try {
            val poster = generatePrintablePosterBitmap(payload, label)
            val dir = File(context.cacheDir, "seals")
            dir.mkdirs()
            val file = File(dir, "sl_tasker_seal.png")
            FileOutputStream(file).use { out ->
                poster.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "SL Tasker Physical Seal: $label ($payload)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Save or Print QR Seal").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {}
    }
}
