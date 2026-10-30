package com.neurasamu.build.sl_tasker.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.data.player.PlayerProfile
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    fun share(ctx: Context, stats: PlayerStatsEntity, profile: PlayerProfile?) {
        try {
            val bitmap = generate(ctx, stats, profile)
            val dir = File(ctx.cacheDir, "share")
            dir.mkdirs()
            val file = File(dir, "sl_tasker_profile.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, buildCaption(stats, profile))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(share, "Share Player Card").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {}
    }

    private fun buildCaption(stats: PlayerStatsEntity, profile: PlayerProfile?): String {
        val name = profile?.name?.uppercase() ?: "PLAYER"
        return "⚔️ $name — Level ${stats.level} · Rank ${rank(stats.level)} · ${stats.totalCompleted} quests cleared · Streak ${stats.streak}🔥 · HP ${stats.health}/100"
    }

    private fun rank(level: Int): String = when {
        level >= 20 -> "S"
        level >= 15 -> "A"
        level >= 10 -> "B"
        level >= 5 -> "C"
        level >= 3 -> "D"
        else -> "E"
    }

    private fun generate(
        ctx: Context,
        stats: PlayerStatsEntity,
        profile: PlayerProfile?
    ): Bitmap {
        val w = 1080
        val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Background gradient
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#050A18"), Color.parseColor("#0A1430"), Color.parseColor("#050A18")),
                null, Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        // Radial glow
        val glow = Paint().apply {
            shader = android.graphics.RadialGradient(
                w / 2f, 320f, 560f,
                intArrayOf(Color.parseColor("#4A90FF80"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(w / 2f, 320f, 560f, glow)

        // Notched card
        val cardPaint = Paint().apply {
            color = Color.parseColor("#0F1830")
            isAntiAlias = true
        }
        val notch = 60f
        val cardPath = android.graphics.Path().apply {
            val left = 80f
            val top = 260f
            val right = (w - 80).toFloat()
            val bottom = (h - 120).toFloat()
            moveTo(left, top)
            lineTo(right - notch, top)
            lineTo(right, top + notch)
            lineTo(right, bottom - 40)
            lineTo(right - 40, bottom)
            lineTo(left + 40, bottom)
            lineTo(left, bottom - 40)
            close()
        }
        c.drawPath(cardPath, cardPaint)

        // Card border
        val border = Paint().apply {
            color = Color.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        c.drawPath(cardPath, border)

        // Top gold hairline
        val gold = Paint().apply {
            color = Color.parseColor("#FBBF24")
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(140f, 261f, 340f, 261f, gold)

        // Text helpers
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val black = Typeface.create("sans-serif-black", Typeface.BOLD)

        // Brand
        val brandPaint = Paint().apply {
            color = Color.parseColor("#4A90FF")
            textSize = 36f
            typeface = bold
            letterSpacing = 0.2f
        }
        c.drawText("SL TASKER", 140f, 200f, brandPaint)

        val sysPaint = Paint().apply {
            color = Color.parseColor("#5A6785")
            textSize = 24f
            typeface = bold
        }
        c.drawText("PLAYER STATUS", 140f, 235f, sysPaint)

        // Rank circle
        val rankVal = rank(stats.level)
        val rankColor = when (rankVal) {
            "S" -> "#FBBF24"
            "A" -> "#F59E0B"
            "B" -> "#8B5CF6"
            "C" -> "#3B82F6"
            "D" -> "#10B981"
            else -> "#94A3B8"
        }
        val rankCircle = Paint().apply {
            color = Color.parseColor(rankColor)
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        c.drawCircle(w / 2f, 450f, 130f, rankCircle)

        val rankFill = Paint().apply {
            color = Color.parseColor(rankColor + "40")
        }
        c.drawCircle(w / 2f, 450f, 126f, rankFill)

        val rankText = Paint().apply {
            color = Color.parseColor(rankColor)
            textSize = 140f
            typeface = black
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        c.drawText(rankVal, w / 2f, 510f, rankText)

        // Name
        val name = profile?.name?.uppercase() ?: "PLAYER"
        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            typeface = black
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        c.drawText(name, w / 2f, 680f, namePaint)

        // Player ID
        val idPaint = Paint().apply {
            color = Color.parseColor("#8B9AB8")
            textSize = 28f
            typeface = bold
            textAlign = Paint.Align.CENTER
        }
        if (!profile?.playerId.isNullOrBlank()) {
            c.drawText(profile.playerId, w / 2f, 725f, idPaint)
        }

        // Level + stats row
        val levelPaint = Paint().apply {
            color = Color.parseColor("#FBBF24")
            textSize = 42f
            typeface = black
            textAlign = Paint.Align.CENTER
        }
        c.drawText("LEVEL ${stats.level}", w / 2f, 800f, levelPaint)

        // Stats rows
        val rowStartY = 900f
        val rowHeight = 90f
        val labelPaint = Paint().apply {
            color = Color.parseColor("#5A6785")
            textSize = 30f
            typeface = bold
        }
        val valuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            typeface = black
            textAlign = Paint.Align.RIGHT
        }

        val stats_list = listOf(
            "HP" to "${stats.health}/100",
            "EP" to "${stats.ep}",
            "Streak" to "${stats.streak} 🔥",
            "Completed" to "${stats.totalCompleted}",
            "Missed" to "${stats.totalMissed}"
        )

        stats_list.forEachIndexed { i, (label, value) ->
            val y = rowStartY + i * rowHeight
            c.drawText(label.uppercase(), 140f, y, labelPaint)
            c.drawText(value, (w - 140).toFloat(), y, valuePaint)
        }

        // Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#4A90FF")
            textSize = 26f
            typeface = bold
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        c.drawText("NEURASAMU.COM", w / 2f, (h - 40).toFloat(), footerPaint)

        return bmp
    }
}
