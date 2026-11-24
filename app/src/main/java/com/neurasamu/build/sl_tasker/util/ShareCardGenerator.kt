package com.neurasamu.build.sl_tasker.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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

enum class FlexCardTemplate(
    val id: String,
    val title: String,
    val assetFileName: String?,
    val isProOnly: Boolean,
    val topColor: String,
    val midColor: String,
    val botColor: String,
    val strokeColor: String,
    val glowColor: String
) {
    STANDARD_FREE(
        id = "standard_free",
        title = "Hunter Standard",
        assetFileName = null,
        isProOnly = false,
        topColor = "#E0F4FF",
        midColor = "#4DC3FF",
        botColor = "#0066FF",
        strokeColor = "#A8E2FF",
        glowColor = "#00AAFF"
    ),
    SOLO_LEVELING(
        id = "solo_leveling",
        title = "Shadow Monarch",
        assetFileName = "flex_cards/solo leveling card sl tasker.jpg",
        isProOnly = true,
        topColor = "#F4E3FF",
        midColor = "#C471FF",
        botColor = "#6E1CD4",
        strokeColor = "#DFB0FF",
        glowColor = "#A855F7"
    ),
    DEFAULT_PRO(
        id = "default_pro",
        title = "Pro Cyan",
        assetFileName = "flex_cards/default pro card sl tasker.jpg",
        isProOnly = true,
        topColor = "#E2FFF9",
        midColor = "#5FF7DF",
        botColor = "#12B6CF",
        strokeColor = "#9FFFF2",
        glowColor = "#00F5DC"
    ),
    GOLD_BLACK(
        id = "gold_black",
        title = "Obsidian Gold",
        assetFileName = "flex_cards/gold black card sl tasker.jpg",
        isProOnly = true,
        topColor = "#FFFBE0",
        midColor = "#FFD76A",
        botColor = "#D48C1C",
        strokeColor = "#FFF0B3",
        glowColor = "#FFB700"
    ),
    GOLDEN(
        id = "golden",
        title = "Imperial Gold",
        assetFileName = "flex_cards/golden card sl tasker.jpg",
        isProOnly = true,
        topColor = "#FFFBE0",
        midColor = "#FFD76A",
        botColor = "#D48C1C",
        strokeColor = "#FFF0B3",
        glowColor = "#FFB700"
    ),
    GOLDEN_FIRE(
        id = "golden_fire",
        title = "Solar Flame",
        assetFileName = "flex_cards/golden fire card sl tasker.jpg",
        isProOnly = true,
        topColor = "#FFF5D6",
        midColor = "#FFB347",
        botColor = "#D9531E",
        strokeColor = "#FFE0A3",
        glowColor = "#FF8C00"
    ),
    GREEN_FIRE(
        id = "green_fire",
        title = "Emerald Blaze",
        assetFileName = "flex_cards/green fire card sl tasker.jpg",
        isProOnly = true,
        topColor = "#E0FFEC",
        midColor = "#43F08B",
        botColor = "#0B9E4B",
        strokeColor = "#A2FFC7",
        glowColor = "#00E676"
    ),
    GREEN(
        id = "green",
        title = "Emerald Aura",
        assetFileName = "flex_cards/green card sl tasker.jpg",
        isProOnly = true,
        topColor = "#E0FFEC",
        midColor = "#43F08B",
        botColor = "#0B9E4B",
        strokeColor = "#A2FFC7",
        glowColor = "#00E676"
    ),
    BLUE(
        id = "blue",
        title = "Cobalt Surge",
        assetFileName = "flex_cards/blue card sl tasker.jpg",
        isProOnly = true,
        topColor = "#E0F4FF",
        midColor = "#4DC3FF",
        botColor = "#0066FF",
        strokeColor = "#A8E2FF",
        glowColor = "#00AAFF"
    ),
    PURPLE(
        id = "purple",
        title = "Abyssal Violet",
        assetFileName = "flex_cards/purple card sl tasker.jpg",
        isProOnly = true,
        topColor = "#F4E3FF",
        midColor = "#C471FF",
        botColor = "#6E1CD4",
        strokeColor = "#DFB0FF",
        glowColor = "#A855F7"
    ),
    RED(
        id = "red",
        title = "Crimson Fury",
        assetFileName = "flex_cards/red card sl tasker.jpg",
        isProOnly = true,
        topColor = "#FFE3E8",
        midColor = "#FF5277",
        botColor = "#B80F38",
        strokeColor = "#FF9EB2",
        glowColor = "#FF1E56"
    ),
    SOFT(
        id = "soft",
        title = "Ivory Prestige",
        assetFileName = "flex_cards/soft card sl tasker.jpg",
        isProOnly = true,
        topColor = "#FFF0F5",
        midColor = "#F48FB1",
        botColor = "#AD1457",
        strokeColor = "#F8BBD0",
        glowColor = "#EC407A"
    )
}

object ShareCardGenerator {

    const val STORE_FALLBACK_URL = "https://neurasamu.com/store"

    fun encodeFlexPayload(
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int,
        weeklySteps: Int
    ): String {
        val safeName = (profile?.name ?: "HUNTER")
            .replace("|", " ")
            .replace("&", " ")
            .trim()
            .ifBlank { "HUNTER" }
            .take(18)
        val shame = if (stats.isShamed()) "CHEATER DETECTED" else ""
        val core = "SLFLEX|v1|$safeName|${stats.level}|${rank(stats.level)}|$weeklyQuests|$weeklySteps|${stats.streak}|$shame"
        // Dual-purpose URL: external scanners open https://neurasamu.com/store, while SL Tasker parses SLFLEX payload
        return "$STORE_FALLBACK_URL?flex=${android.net.Uri.encode(core)}"
    }

    fun parseFlexPayload(raw: String): FriendFlexStats? {
        val trimmed = raw.trim()
        val flexStr = when {
            trimmed.startsWith("SLFLEX|") -> trimmed
            trimmed.contains("flex=") -> {
                val after = trimmed.substringAfter("flex=").substringBefore("&")
                android.net.Uri.decode(after)
            }
            else -> return null
        }
        val parts = flexStr.split("|")
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

    fun loadPreviewThumbnail(ctx: Context, template: FlexCardTemplate): Bitmap? {
        val assetPath = template.assetFileName ?: return null
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            ctx.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Throwable) {
            null
        }
    }

    fun generatePreviewBitmap(
        ctx: Context,
        template: FlexCardTemplate,
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int,
        weeklySteps: Int
    ): Bitmap {
        return if (template.assetFileName != null) {
            generateProTemplateCard(ctx, template, stats, profile, weeklyQuests, weeklySteps)
                ?: generateFreeStandardCard(ctx, stats, profile, weeklyQuests, weeklySteps)
        } else {
            generateFreeStandardCard(ctx, stats, profile, weeklyQuests, weeklySteps)
        }
    }

    fun share(
        ctx: Context,
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int = stats.totalCompleted,
        weeklySteps: Int = StepTrackerManager.weeklySteps.value,
        template: FlexCardTemplate = FlexCardTemplate.STANDARD_FREE
    ) {
        try {
            val bitmap = generatePreviewBitmap(ctx, template, stats, profile, weeklyQuests, weeklySteps)
            val dir = File(ctx.cacheDir, "share")
            dir.mkdirs()
            val file = File(dir, "sl_tasker_flex_card.png")
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
        val shameTag = if (stats.isShamed()) " [⚠ CHEATER DETECTED]" else ""
        return "$name$shameTag — Level ${stats.level} (Rank ${rank(stats.level)}) · $weeklyQuests weekly quests · $weeklySteps weekly steps · Streak ${stats.streak}d · $STORE_FALLBACK_URL"
    }

    fun rank(level: Int): String = when {
        level >= 20 -> "S"
        level >= 15 -> "A"
        level >= 10 -> "B"
        level >= 5 -> "C"
        level >= 3 -> "D"
        else -> "E"
    }

    /**
     * Exact port of card generator.html Canvas engine onto the Pro template artwork.
     */
    private fun generateProTemplateCard(
        ctx: Context,
        template: FlexCardTemplate,
        stats: PlayerStatsEntity,
        profile: PlayerProfile?,
        weeklyQuests: Int,
        weeklySteps: Int
    ): Bitmap? {
        val assetPath = template.assetFileName ?: return null
        val baseBmp = try {
            val opts = BitmapFactory.Options().apply { inMutable = true }
            ctx.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Throwable) {
            null
        } ?: return null

        val bmp = if (baseBmp.isMutable) baseBmp else baseBmp.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(bmp)
        val W = bmp.width.toFloat()
        val H = bmp.height.toFloat()

        val orbitron = Typeface.create("sans-serif-black", Typeface.BOLD)
        val chakra = Typeface.create("sans-serif-condensed", Typeface.BOLD)

        // 1. Center Shield Rank Letter (y = 0.285H)
        val rankStr = rank(stats.level)
        val rankY = H * 0.285f
        val rankSize = if (rankStr.length > 1) W * 0.088f else W * 0.118f
        val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = orbitron
            textSize = rankSize
            textAlign = Paint.Align.CENTER
            setShadowLayer(W * 0.025f, 0f, 0f, Color.parseColor(template.glowColor))
            shader = LinearGradient(
                0f, rankY - rankSize * 0.52f,
                0f, rankY + rankSize * 0.52f,
                intArrayOf(
                    Color.parseColor(template.topColor),
                    Color.parseColor(template.midColor),
                    Color.parseColor(template.botColor)
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val rankFm = rankPaint.fontMetrics
        val rankBaseline = rankY - (rankFm.ascent + rankFm.descent) / 2f
        c.drawText(rankStr, W * 0.500f, rankBaseline, rankPaint)

        val rankStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = orbitron
            textSize = rankSize
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = W * 0.0038f
            color = Color.parseColor(template.strokeColor)
        }
        c.drawText(rankStr, W * 0.500f, rankBaseline, rankStroke)

        // 2. Player Name (Metallic Gold @ y = 0.408H)
        val playerName = (profile?.name ?: "HUNTER").trim().uppercase().take(18)
        drawGoldText(
            c = c,
            text = playerName,
            x = W * 0.500f,
            centerY = H * 0.408f,
            fontSize = W * 0.056f,
            tf = orbitron,
            align = Paint.Align.CENTER,
            glowRadius = W * 0.018f
        )

        // 3. Level Text (@ y = 0.451H)
        drawGoldText(
            c = c,
            text = "LEVEL ${stats.level}",
            x = W * 0.500f,
            centerY = H * 0.451f,
            fontSize = W * 0.030f,
            tf = chakra,
            align = Paint.Align.CENTER,
            glowRadius = W * 0.012f
        )

        // 3b. Cheater Penalty Stamp if Anti-Cheat active (12h)
        if (stats.isShamed()) {
            val stampY = H * 0.492f
            val stampBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D92B050A") }
            val stampBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF3B5C")
                style = Paint.Style.STROKE
                strokeWidth = W * 0.003f
            }
            val rect = RectF(W * 0.25f, stampY - H * 0.018f, W * 0.75f, stampY + H * 0.018f)
            c.drawRoundRect(rect, W * 0.012f, W * 0.012f, stampBg)
            c.drawRoundRect(rect, W * 0.012f, W * 0.012f, stampBorder)
            val stampTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6B81")
                textSize = W * 0.024f
                typeface = orbitron
                textAlign = Paint.Align.CENTER
            }
            val fm = stampTxt.fontMetrics
            c.drawText("⚠ CHEATER DETECTED", W * 0.500f, stampY - (fm.ascent + fm.descent) / 2f, stampTxt)
        }

        // 4. Four Right-Aligned Stat Rows (x = 0.620W; y = 0.554, 0.606, 0.659, 0.712)
        val statX = W * 0.620f
        val statSize = W * 0.035f
        val rows = listOf(
            "$weeklyQuests" to (H * 0.554f),
            "$weeklySteps" to (H * 0.606f),
            "${stats.streak}d" to (H * 0.659f),
            "${stats.totalCompleted}" to (H * 0.712f)
        )
        rows.forEach { (value, rowY) ->
            drawGoldText(
                c = c,
                text = value,
                x = statX,
                centerY = rowY,
                fontSize = statSize,
                tf = chakra,
                align = Paint.Align.RIGHT,
                glowRadius = W * 0.010f
            )
        }

        // 5. Dynamic 10-Segment Slanted Health Bar & Readout (@ y = 0.764H)
        val ratio = (stats.health.coerceIn(0, 100)) / 100f
        val centerY = H * 0.764f
        val barH = H * 0.0225f
        val barTop = centerY - barH / 2f
        val slant = W * 0.0115f
        val totalBars = 10
        val filledBars = Math.round(ratio * totalBars)
        val startX = W * 0.352f
        val endX = W * 0.494f
        val step = (endX - startX) / totalBars
        val barW = step * 0.60f

        for (i in 0 until totalBars) {
            val bx = startX + i * step
            val path = Path().apply {
                moveTo(bx + slant, barTop)
                lineTo(bx + slant + barW, barTop)
                lineTo(bx + barW, barTop + barH)
                lineTo(bx, barTop + barH)
                close()
            }
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            if (i < filledBars) {
                barPaint.shader = LinearGradient(
                    0f, barTop, 0f, barTop + barH,
                    intArrayOf(
                        Color.parseColor("#C2FFFF"),
                        Color.parseColor("#3BE8FF"),
                        Color.parseColor("#00A3E8")
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                barPaint.setShadowLayer(W * 0.008f, 0f, 0f, Color.parseColor("#00E5FF"))
            } else {
                barPaint.color = Color.argb(92, 42, 92, 138)
            }
            c.drawPath(path, barPaint)
        }

        val hpText = "${stats.health}/100"
        val hpFontSize = W * 0.033f
        val hpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = chakra
            textSize = hpFontSize
            textAlign = Paint.Align.RIGHT
            setShadowLayer(W * 0.009f, 0f, 0f, Color.argb(190, 0, 229, 255))
            shader = LinearGradient(
                0f, centerY - hpFontSize * 0.5f,
                0f, centerY + hpFontSize * 0.5f,
                intArrayOf(
                    Color.parseColor("#EEFDFF"),
                    Color.parseColor("#8AF4FF"),
                    Color.parseColor("#39CBFF")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val hpFm = hpPaint.fontMetrics
        c.drawText(hpText, W * 0.620f, centerY - (hpFm.ascent + hpFm.descent) / 2f, hpPaint)

        // 6. Scannable QR Plate (@ cx = 0.784W, cy = 0.667H, size = 0.198W)
        val qrSize = W * 0.198f
        val cx = W * 0.784f
        val cy = H * 0.667f
        val left = cx - qrSize / 2f
        val top = cy - qrSize / 2f
        val plateRect = RectF(left, top, left + qrSize, top + qrSize)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2F0FA")
            setShadowLayer(W * 0.012f, 0f, 0f, Color.argb(120, 0, 210, 255))
        }
        c.drawRoundRect(plateRect, qrSize * 0.035f, qrSize * 0.035f, platePaint)

        val pad = qrSize * 0.06f
        val innerPx = (qrSize - pad * 2f).toInt().coerceAtLeast(64)
        val payload = encodeFlexPayload(stats, profile, weeklyQuests, weeklySteps)
        val qrBmp = runCatching { QrBarcodeHelper.generateQrBitmap(payload, innerPx) }.getOrNull()
        if (qrBmp != null) {
            c.drawBitmap(qrBmp, left + pad, top + pad, null)
        }

        return bmp
    }

    private fun drawGoldText(
        c: Canvas,
        text: String,
        x: Float,
        centerY: Float,
        fontSize: Float,
        tf: Typeface,
        align: Paint.Align,
        glowRadius: Float
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = fontSize
            textAlign = align
            color = Color.parseColor("#3B2406")
            setShadowLayer(fontSize * 0.16f, 0f, fontSize * 0.05f, Color.argb(215, 0, 0, 0))
        }
        val fm = shadowPaint.fontMetrics
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        c.drawText(text, x, baseline, shadowPaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = fontSize
            textAlign = align
            setShadowLayer(glowRadius, 0f, 0f, Color.argb(185, 255, 196, 72))
            shader = LinearGradient(
                0f, centerY - fontSize * 0.55f,
                0f, centerY + fontSize * 0.55f,
                intArrayOf(
                    Color.parseColor("#FFF9D6"),
                    Color.parseColor("#FFDC7D"),
                    Color.parseColor("#DDA038"),
                    Color.parseColor("#A66916")
                ),
                floatArrayOf(0f, 0.42f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawText(text, x, baseline, fillPaint)
    }

    /**
     * Free Tier Standard Card (available to all users without Pro).
     */
    private fun generateFreeStandardCard(
        ctx: Context,
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

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F1830") }
        val notch = 60f
        val cardPath = Path().apply {
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

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        c.drawPath(cardPath, border)

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val black = Typeface.create("sans-serif-black", Typeface.BOLD)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60A5FA")
            textSize = 36f
            typeface = bold
            letterSpacing = 0.2f
        }
        c.drawText("SL TASKER", 132f, 175f, brandPaint)

        val sysPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8B9AB8")
            textSize = 23f
            typeface = bold
        }
        c.drawText("HUNTER FLEX CARD (FREE)", 132f, 208f, sysPaint)

        val rankVal = rank(stats.level)
        val rankColor = when (rankVal) {
            "S" -> "#FBBF24"
            "A" -> "#F59E0B"
            "B" -> "#8B5CF6"
            "C" -> "#3B82F6"
            "D" -> "#10B981"
            else -> "#94A3B8"
        }
        val rankCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(rankColor)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        c.drawCircle(w / 2f, 405f, 118f, rankCircle)

        val rankFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(rankColor + "40") }
        c.drawCircle(w / 2f, 405f, 114f, rankFill)

        val rankText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(rankColor)
            textSize = 128f
            typeface = black
            textAlign = Paint.Align.CENTER
        }
        c.drawText(rankVal, w / 2f, 458f, rankText)

        val name = profile?.name?.uppercase() ?: "HUNTER"
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 58f
            typeface = black
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }
        c.drawText(name, w / 2f, 605f, namePaint)

        val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FBBF24")
            textSize = 38f
            typeface = black
            textAlign = Paint.Align.CENTER
        }
        c.drawText("LEVEL ${stats.level}", w / 2f, 660f, levelPaint)

        if (stats.isShamed()) {
            val shameBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B0A0A") }
            val shameBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EF4444")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val rect = RectF(160f, 682f, (w - 160).toFloat(), 732f)
            c.drawRoundRect(rect, 14f, 14f, shameBg)
            c.drawRoundRect(rect, 14f, 14f, shameBorder)
            val shameTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6B6B")
                textSize = 24f
                typeface = bold
                textAlign = Paint.Align.CENTER
            }
            c.drawText("⚠ CHEATER DETECTED", w / 2f, 715f, shameTxt)
        }

        val rowStartY = 805f
        val rowHeight = 76f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8B9AB8")
            textSize = 26f
            typeface = bold
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
            val whiteBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            c.drawRoundRect(qrBox, 16f, 16f, whiteBg)
            c.drawBitmap(qrBmp, 725f, 840f, null)

            val scanHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#93C5FD")
                textSize = 20f
                typeface = bold
                textAlign = Paint.Align.CENTER
            }
            c.drawText("SCAN TO COMPARE", 845f, 1122f, scanHintPaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60A5FA")
            textSize = 25f
            typeface = bold
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        c.drawText("SL TASKER · NEURASAMU.COM/STORE", w / 2f, (h - 32).toFloat(), footerPaint)

        return bmp
    }
}
