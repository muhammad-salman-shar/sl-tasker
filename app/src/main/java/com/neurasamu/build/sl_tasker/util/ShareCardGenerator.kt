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

data class FriendFlexStats(
    val name: String,
    val level: Int,
    val rank: String,
    val weeklyQuests: Int,
    val weeklySteps: Int,
    val streak: Int,
    val shameBadge: String
)

object ShareCardGenerator {

    fun encodeFlexPayload(
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int,
        weeklySteps: Int
    ): String {
        val safeName = (profile?.name ?: "HUNTER")
            .replace("|", " ")
            .trim()
            .ifBlank { "HUNTER" }
            .take(20)
        val shame = if (stats.isShamed()) stats.shamingReason.replace("|", " ").take(36) else ""
        return "SLFLEX|v1|$safeName|${stats.level}|${rank(stats.level)}|$weeklyQuests|$weeklySteps|${stats.streak}|$shame"
    }

    fun parseFlexPayload(raw: String): FriendFlexStats? {
        val parts = raw.trim().split("|")
        if (parts.size < 8 || parts[0] != "SLFLEX") return null
        return FriendFlexStats(
            name = parts[2].ifBlank { "FRIEND" },
            level = parts[3].toIntOrNull() ?: 1,
            rank = parts[4].ifBlank { "E" },
            weeklyQuests = parts[5].toIntOrNull() ?: 0,
            weeklySteps = parts[6].toIntOrNull() ?: 0,
            streak = parts[7].toIntOrNull() ?: 0,
            shameBadge = parts.getOrNull(8)?.trim() ?: ""
        )
    }

    fun share(
        ctx: Context,
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int = stats.totalCompleted,
        weeklySteps: Int = StepTrackerManager.weeklySteps.value
    ) {
        try {
            val bitmap = generate(stats, profile, weeklyQuests, weeklySteps)
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
                putExtra(Intent.EXTRA_TEXT, buildCaption(stats, profile, weeklyQuests, weeklySteps))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(share, "Share Flex Card").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Throwable) {}
    }

    private fun buildCaption(
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int,
        weeklySteps: Int
    ): String {
        val name = profile?.name?.uppercase() ?: "HUNTER"
        val shameTag = if (stats.isShamed()) " [⚠ ${stats.shamingReason}]" else ""
        return "$name$shameTag — Level ${stats.level} (Rank ${rank(stats.level)}) · $weeklyQuests weekly quests · $weeklySteps weekly steps · Streak ${stats.streak}d"
    }

    fun rank(level: Int): String = when {
        level >= 20 -> "S"
        level >= 15 -> "A"
        level >= 10 -> "B"
        level >= 5 -> "C"
        level >= 3 -> "D"
        else -> "E"
    }

    private fun generate(
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int,
        weeklySteps: Int
    ): Bitmap {
        val w = 1080
        val h = 1440
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#050A18"), Color.parseColor("#0A1430"), Color.parseColor("#050A18")),
                null, Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val glow = Paint().apply {
            shader = android.graphics.RadialGradient(
                w / 2f, 300f, 540f,
                intArrayOf(Color.parseColor("#4A90FF80"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(w / 2f, 300f, 540f, glow)

        val cardPaint = Paint().apply {
            color = Color.parseColor("#0F1830")
            isAntiAlias = true
        }
        val notch = 60f
        val cardPath = android.graphics.Path().apply {
            val left = 72f
            val top = 230f
            val right = (w - 72).toFloat()
            val bottom = (h - 90).toFloat()
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

        val border = Paint().apply {
            color = Color.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        c.drawPath(cardPath, border)

        val gold = Paint().apply {
            color = Color.parseColor("#FBBF24")
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(132f, 231f, 332f, 231f, gold)

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val black = Typeface.create("sans-serif-black", Typeface.BOLD)

        val brandPaint = Paint().apply {
            color = Color.parseColor("#60A5FA")
            textSize = 36f
            typeface = bold
            letterSpacing = 0.2f
        }
        c.drawText("SL TASKER", 132f, 175f, brandPaint)

        val sysPaint = Paint().apply {
            color = Color.parseColor("#8B9AB8")
            textSize = 23f
            typeface = bold
        }
        c.drawText("PROGRESS FLEX CARD", 132f, 208f, sysPaint)

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
        c.drawCircle(w / 2f, 405f, 118f, rankCircle)

        val rankFill = Paint().apply {
            color = Color.parseColor(rankColor + "40")
        }
        c.drawCircle(w / 2f, 405f, 114f, rankFill)

        val rankText = Paint().apply {
            color = Color.parseColor(rankColor)
            textSize = 128f
            typeface = black
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        c.drawText(rankVal, w / 2f, 458f, rankText)

        val name = profile?.name?.uppercase() ?: "HUNTER"
        val namePaint = Paint().apply {
            textSize = 58f
            typeface = black
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            if (com.neurasamu.build.sl_tasker.security.LicenseManager.isPro()) {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), 0f,
                    intArrayOf(0xFFFFD700.toInt(), 0xFFFFA500.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            } else {
                color = Color.WHITE
            }
        }
        c.drawText(name, w / 2f, 605f, namePaint)

        val levelPaint = Paint().apply {
            color = Color.parseColor("#FBBF24")
            textSize = 38f
            typeface = black
            textAlign = Paint.Align.CENTER
        }
        c.drawText("LEVEL ${stats.level}", w / 2f, 660f, levelPaint)

        if (stats.isShamed()) {
            val shameBg = Paint().apply {
                color = Color.parseColor("#3B0A0A")
                isAntiAlias = true
            }
            val shameBorder = Paint().apply {
                color = Color.parseColor("#EF4444")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val rect = RectF(140f, 682f, (w - 140).toFloat(), 732f)
            c.drawRoundRect(rect, 14f, 14f, shameBg)
            c.drawRoundRect(rect, 14f, 14f, shameBorder)
            val shameTxt = Paint().apply {
                color = Color.parseColor("#FF6B6B")
                textSize = 23f
                typeface = bold
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            c.drawText("⚠ ${stats.shamingReason.uppercase()}", w / 2f, 715f, shameTxt)
        }

        val rowStartY = 805f
        val rowHeight = 76f
        val labelPaint = Paint().apply {
            color = Color.parseColor("#8B9AB8")
            textSize = 26f
            typeface = bold
        }
        val valuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = black
            textAlign = Paint.Align.RIGHT
        }

        val statsList = listOf(
            "WEEKLY QUESTS" to "$weeklyQuests",
            "WEEKLY STEPS" to "$weeklySteps",
            "STREAK" to "${stats.streak}d",
            "TOTAL CLEARED" to "${stats.totalCompleted}",
            "HEALTH" to "${stats.health}/100"
        )

        statsList.forEachIndexed { i, (label, value) ->
            val y = rowStartY + i * rowHeight
            c.drawText(label, 124f, y, labelPaint)
            c.drawText(value, 670f, y, valuePaint)
        }

        val flexPayload = encodeFlexPayload(stats, profile, weeklyQuests, weeklySteps)
        val qrBmp = runCatching { QrBarcodeHelper.generateQrBitmap(flexPayload, 240) }.getOrNull()
        if (qrBmp != null) {
            val qrBox = RectF(715f, 830f, 975f, 1090f)
            val whiteBg = Paint().apply { color = Color.WHITE; isAntiAlias = true }
            c.drawRoundRect(qrBox, 16f, 16f, whiteBg)
            c.drawBitmap(qrBmp, 725f, 840f, null)

            val scanHintPaint = Paint().apply {
                color = Color.parseColor("#93C5FD")
                textSize = 20f
                typeface = bold
                textAlign = Paint.Align.CENTER
            }
            c.drawText("SCAN TO COMPARE", 845f, 1122f, scanHintPaint)
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#60A5FA")
            textSize = 25f
            typeface = bold
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        c.drawText("SL TASKER · NEURASAMU.COM", w / 2f, (h - 32).toFloat(), footerPaint)

        return bmp
    }
}
