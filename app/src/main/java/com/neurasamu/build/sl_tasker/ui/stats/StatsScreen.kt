package com.neurasamu.build.sl_tasker.ui.stats

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.EventEntity
import com.neurasamu.build.sl_tasker.data.model.EventType
import com.neurasamu.build.sl_tasker.data.player.PlayerProfile
import com.neurasamu.build.sl_tasker.data.player.PlayerProfileStore
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.ui.components.QrScannerDialog
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextOnBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.util.FriendFlexStats
import com.neurasamu.build.sl_tasker.util.ShareCardGenerator
import com.neurasamu.build.sl_tasker.util.StepTrackerManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val stats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    val events by statsViewModel.recentEvents.collectAsStateWithLifecycle()
    val weeklyEvents by statsViewModel.weeklyEvents.collectAsStateWithLifecycle()
    val weeklySteps by StepTrackerManager.weeklySteps.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<PlayerProfile?>(null) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showRankHall by remember { mutableStateOf(false) }
    var showScanFriendFlex by remember { mutableStateOf(false) }
    var friendFlex by remember { mutableStateOf<FriendFlexStats?>(null) }
    var showCardSelectSheet by remember { mutableStateOf(false) }
    var showGeneratedCardPreview by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf(com.neurasamu.build.sl_tasker.util.FlexCardTemplate.STANDARD_FREE) }

    val weeklyCompleted = remember(weeklyEvents) {
        weeklyEvents.count { it.type == EventType.OCCURRENCE_COMPLETED }
    }
    val weeklyMissed = remember(weeklyEvents) {
        weeklyEvents.count { it.type == EventType.OCCURRENCE_MISSED }
    }
    val weeklyConsistencyPct = remember(weeklyCompleted, weeklyMissed) {
        val total = weeklyCompleted + weeklyMissed
        if (total == 0) 100 else ((weeklyCompleted * 100) / total).coerceIn(0, 100)
    }
    val rankProgressPct = remember(stats.ep) {
        ((stats.ep * 100) / GamificationEngine.EP_PER_LEVEL).coerceIn(0, 100)
    }

    LaunchedEffect(Unit) {
        try {
            PlayerProfileStore(ctx).profile.collect { p -> profile = p }
        } catch (_: Throwable) { }
    }

    val headerAccent = if (ThemeState.isLight) PrimaryManaBlue else AccentCyan

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(headerAccent.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width / 2, 0f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width / 2, 0f)
            )
            val step = 60f
            val gridColor = headerAccent.copy(alpha = 0.03f)
            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            profile?.let { p ->
                if (p.introCompleted) {
                    item {
                        IdentityCard(
                            profile = p,
                            onEdit = { showEditProfile = true },
                            onCompareFriend = { showScanFriendFlex = true },
                            onShare = {
                                selectedTemplate = if (LicenseManager.isPro()) {
                                    com.neurasamu.build.sl_tasker.util.FlexCardTemplate.SOLO_LEVELING
                                } else {
                                    com.neurasamu.build.sl_tasker.util.FlexCardTemplate.STANDARD_FREE
                                }
                                showCardSelectSheet = true
                            }
                        )
                    }
                }
            }

            item {
                Column {
                    Text(
                        "HUNTER PROFILE",
                        color = headerAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Permanent progression record",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            if (stats.isShamed()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DangerPenaltyRed.copy(alpha = 0.16f))
                            .border(1.5.dp, DangerPenaltyRed, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Shaming Badge",
                            tint = DangerPenaltyRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CHEATER DETECTED (12H PENALTY)",
                                color = DangerPenaltyRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stats.shamingReason,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                RankCard(
                    level = stats.level,
                    streak = stats.streak,
                    onClick = { showRankHall = true }
                )
            }

            item {
                BarsCard(
                    hp = stats.health,
                    ep = stats.ep
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallStatBox(
                        value = "${stats.totalCompleted}",
                        label = "COMPLETED",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatBox(
                        value = "${stats.totalMissed}",
                        label = "MISSED",
                        color = DangerPenaltyRed,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatBox(
                        value = "${stats.totalRecoveries}",
                        label = "RECOVERIES",
                        color = GoldWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // WEEKLY PROGRESS REPORT
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard.copy(alpha = 0.90f))
                        .border(1.dp, PrimaryManaBlue.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WEEKLY PROGRESS REPORT",
                            color = PrimaryManaBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryManaBlue)
                                .clickable { showScanFriendFlex = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "COMPARE FRIEND QR",
                                color = TextOnBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallStatBox(
                            value = "$weeklyCompleted",
                            label = "WEEK QUESTS",
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        SmallStatBox(
                            value = "$weeklySteps",
                            label = "WEEK STEPS",
                            color = headerAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallStatBox(
                            value = "$weeklyConsistencyPct%",
                            label = "CONSISTENCY",
                            color = GoldWarning,
                            modifier = Modifier.weight(1f)
                        )
                        SmallStatBox(
                            value = "$rankProgressPct%",
                            label = "RANK PROGRESS",
                            color = PrimaryManaBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RECENT ACTIVITY (TODAY)",
                        color = headerAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Resets 12:00 AM",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            if (events.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No activity yet in today's cycle.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                items(items = events.take(10), key = { it.id }) { event ->
                    ActivityRow(event)
                }
            }
        }

        if (showRankHall) {
            RankShowcaseDialog(
                currentLevel = stats.level,
                onDismiss = { showRankHall = false }
            )
        }

        if (showScanFriendFlex) {
            QrScannerDialog(
                title = "SCAN FRIEND'S FLEX CARD",
                subtitle = "Scan the QR code on your friend's SL Tasker Progress Flex Card to compare stats side-by-side",
                expectedCode = null,
                onDismiss = { showScanFriendFlex = false },
                onScanned = { scanned ->
                    showScanFriendFlex = false
                    val parsed = ShareCardGenerator.parseFlexPayload(scanned)
                    if (parsed != null) {
                        friendFlex = parsed
                    } else {
                        Toast.makeText(
                            ctx,
                            "Not a valid SL Tasker Flex Card QR.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        friendFlex?.let { friend ->
            val myName = profile?.name?.uppercase()?.ifBlank { "YOU" } ?: "YOU"
            val myRank = ShareCardGenerator.rank(stats.level)
            val myScore = stats.level * 1000 + weeklyCompleted * 50 + (weeklySteps / 100) + stats.streak * 30 - (if (stats.isShamed()) 5000 else 0)
            val friendScore = friend.level * 1000 + friend.weeklyQuests * 50 + (friend.weeklySteps / 100) + friend.streak * 30 - (if (friend.shameBadge.isNotBlank()) 5000 else 0)
            val iAmWinner = myScore >= friendScore

            AlertDialog(
                onDismissRequest = { friendFlex = null },
                containerColor = DarkCard,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "⚔ HUNTER DUEL COMPARISON",
                        color = AccentGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(
                                        width = if (iAmWinner) 2.dp else 1.dp,
                                        color = if (iAmWinner) AccentGold else DarkBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (iAmWinner) {
                                    Text("👑 BADSHAH", color = AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    text = if (iAmWinner) "👑 $myName" else myName,
                                    color = if (iAmWinner) AccentGold else PrimaryManaBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("Rank $myRank · Lv.${stats.level}", color = TextPrimary, fontSize = 11.sp)
                                Text("$weeklyCompleted Quests", color = TextSecondary, fontSize = 10.sp)
                                Text("$weeklySteps Steps", color = TextSecondary, fontSize = 10.sp)
                                Text("${stats.streak}d Streak", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (stats.isShamed()) {
                                    Text("⚠ CHEATER", color = DangerPenaltyRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(
                                        width = if (!iAmWinner) 2.dp else 1.dp,
                                        color = if (!iAmWinner) AccentGold else DarkBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!iAmWinner) {
                                    Text("👑 BADSHAH", color = AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    text = if (!iAmWinner) "👑 ${friend.name.uppercase()}" else friend.name.uppercase(),
                                    color = if (!iAmWinner) AccentGold else AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("Rank ${friend.rank} · Lv.${friend.level}", color = TextPrimary, fontSize = 11.sp)
                                Text("${friend.weeklyQuests} Quests", color = TextSecondary, fontSize = 10.sp)
                                Text("${friend.weeklySteps} Steps", color = TextSecondary, fontSize = 10.sp)
                                Text("${friend.streak}d Streak", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (friend.shameBadge.isNotBlank()) {
                                    Text("⚠ CHEATER", color = DangerPenaltyRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Text(
                            text = if (iAmWinner)
                                "👑 $myName wears the Crown of Discipline!"
                            else
                                "👑 ${friend.name.uppercase()} takes the Crown! Complete more quests to reclaim it.",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { friendFlex = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryManaBlue, contentColor = TextOnBlue)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }

        // STEP 1: Bottom Sheet -> "SELECT YOUR CARD" -> "GENERATE CARD"
        if (showCardSelectSheet) {
            val isProUser by LicenseManager.isProFlow.collectAsState()
            val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showCardSelectSheet = false },
                sheetState = sheetState,
                containerColor = DarkCard,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SELECT YOUR CARD",
                        color = AccentGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Choose a Hunter Card design, then tap Generate Card",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(com.neurasamu.build.sl_tasker.util.FlexCardTemplate.entries) { tpl ->
                            val isSel = selectedTemplate == tpl
                            val locked = tpl.isProOnly && !isProUser
                            val thumb = remember(tpl) { ShareCardGenerator.loadPreviewThumbnail(ctx, tpl) }
                            Column(
                                modifier = Modifier
                                    .width(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) AccentGold.copy(alpha = 0.18f) else DarkSurface)
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) AccentGold else DarkBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (locked) {
                                            com.neurasamu.build.sl_tasker.security.PaywallTrigger.show(
                                                "${tpl.title} is a PRO Flex Card! Upgrade to unlock all 11 Luxury Hunter Cards."
                                            )
                                        } else {
                                            selectedTemplate = tpl
                                        }
                                    }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (thumb != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = tpl.title,
                                        modifier = Modifier
                                            .size(width = 76.dp, height = 102.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 76.dp, height = 102.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("FREE", color = PrimaryManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = tpl.title,
                                    color = if (isSel) AccentGold else TextPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (locked) "PRO 🔒" else if (tpl.isProOnly) "PRO" else "FREE",
                                    color = if (tpl.isProOnly) AccentGold else SuccessGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            showCardSelectSheet = false
                            showGeneratedCardPreview = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "GENERATE CARD",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        // STEP 2: Generated Card Preview with Top-Right Share + Bottom WhatsApp, TikTok, YouTube, Facebook, Other
        if (showGeneratedCardPreview) {
            val generatedBmp = remember(selectedTemplate, stats, profile, weeklyCompleted, weeklySteps) {
                ShareCardGenerator.generatePreviewBitmap(
                    ctx = ctx,
                    template = selectedTemplate,
                    stats = stats,
                    profile = profile,
                    weeklyQuests = weeklyCompleted,
                    weeklySteps = weeklySteps
                )
            }
            androidx.compose.ui.window.Dialog(onDismissRequest = { showGeneratedCardPreview = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(DarkCard)
                        .border(1.5.dp, AccentGold.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showGeneratedCardPreview = false }) {
                            Text("CLOSE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "YOUR FLEX CARD",
                            color = AccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                ShareCardGenerator.share(
                                    ctx = ctx,
                                    stats = stats,
                                    profile = profile,
                                    weeklyQuests = weeklyCompleted,
                                    weeklySteps = weeklySteps,
                                    template = selectedTemplate
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share Card",
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    androidx.compose.foundation.Image(
                        bitmap = generatedBmp.asImageBitmap(),
                        contentDescription = "Generated Flex Card",
                        modifier = Modifier
                            .height(320.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "SHARE TO",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    val socialTargets = listOf(
                        Triple("WhatsApp", Color(0xFF25D366), listOf("com.whatsapp", "com.whatsapp.w4b")),
                        Triple("TikTok", Color(0xFF00F2EA), listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")),
                        Triple("YouTube", Color(0xFFFF0000), listOf("com.google.android.youtube")),
                        Triple("Facebook", Color(0xFF1877F2), listOf("com.facebook.katana", "com.facebook.lite")),
                        Triple("Other", PrimaryManaBlue, emptyList())
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        socialTargets.forEach { (appName, badgeColor, pkgs) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        ShareCardGenerator.share(
                                            ctx = ctx,
                                            stats = stats,
                                            profile = profile,
                                            weeklyQuests = weeklyCompleted,
                                            weeklySteps = weeklySteps,
                                            template = selectedTemplate,
                                            targetPackages = pkgs
                                        )
                                    }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appName.take(2).uppercase(),
                                        color = badgeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = appName,
                                    color = TextPrimary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        val currentProfile = profile
        if (showEditProfile && currentProfile != null) {
            EditProfileDialog(
                profile = currentProfile,
                onDismiss = { showEditProfile = false },
                onSave = { name, gender, age, playerId ->
                    scope.launch {
                        try {
                            PlayerProfileStore(ctx).save(name, gender, age, playerId)
                        } catch (_: Throwable) {}
                        showEditProfile = false
                    }
                }
            )
        }
    }
}

@Composable
private fun FlexCompareRow(metric: String, myVal: String, friendVal: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(myVal, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(metric, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(friendVal, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditProfileDialog(
    profile: PlayerProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var playerId by remember { mutableStateOf(profile.playerId) }
    var gender by remember { mutableStateOf(profile.gender.ifBlank { "Male" }) }
    var ageText by remember { mutableStateOf(profile.age.toString()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryManaBlue,
        unfocusedBorderColor = DarkBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = PrimaryManaBlue,
        unfocusedLabelColor = TextSecondary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                "EDIT HUNTER PROFILE",
                color = PrimaryManaBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    label = { Text("Hunter Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = playerId,
                    onValueChange = { if (it.length <= 20) playerId = it },
                    label = { Text("Player ID") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Age (12-90)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        val sel = gender.equals(g, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) PrimaryManaBlue.copy(alpha = 0.22f) else DarkSurface)
                                .border(
                                    1.dp,
                                    if (sel) PrimaryManaBlue else DarkBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { gender = g }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                g.uppercase(),
                                color = if (sel) PrimaryManaBlue else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeName = name.trim().ifBlank { profile.name.ifBlank { "HUNTER" } }
                    val safeId = playerId.trim().ifBlank { profile.playerId.ifBlank { safeName } }
                    val safeAge = ageText.toIntOrNull()?.coerceIn(12, 90) ?: profile.age
                    onSave(safeName, gender, safeAge, safeId)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryManaBlue,
                    contentColor = TextOnBlue
                )
            ) {
                Text("SAVE", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun IdentityCard(
    profile: PlayerProfile,
    onEdit: () -> Unit,
    onCompareFriend: () -> Unit = {},
    onShare: () -> Unit
) {
    val isPro by LicenseManager.isProFlow.collectAsState()
    val headerAccent = if (ThemeState.isLight) PrimaryManaBlue else AccentCyan
    val genderStr = when (profile.gender.lowercase()) {
        "male" -> "MALE"
        "female" -> "FEMALE"
        else -> profile.gender.uppercase()
    }
    val goldBorder = Brush.linearGradient(
        listOf(Color(0xFFFFF5B8), Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFD700))
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.90f))
            .border(
                width = if (isPro) 1.3.dp else 1.dp,
                brush = if (isPro) goldBorder else Brush.linearGradient(
                    listOf(headerAccent.copy(alpha = 0.35f), DarkBorder)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PLAYER IDENTITY",
                color = if (isPro) AccentGold else headerAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCompareFriend, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = "Scan Friend Flex QR",
                    tint = headerAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Edit Profile",
                    tint = if (isPro) AccentGold else headerAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Share Flex Card",
                    tint = headerAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (isPro) {
            LuxuryNameplate(
                name = profile.name.uppercase().ifBlank { "HUNTER" },
                isPro = true
            )
        } else {
            IdentityField("NAME", profile.name.uppercase().ifBlank { "HUNTER" })
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                IdentityField("PLAYER ID", profile.playerId, accent = true)
            }
            Column(modifier = Modifier.weight(1f)) {
                IdentityField("GENDER", genderStr)
            }
            Column(modifier = Modifier.weight(0.6f)) {
                IdentityField("AGE", profile.age.toString())
            }
        }
    }
}

@Composable
private fun LuxuryNameplate(name: String, isPro: Boolean) {
    val goldBorder = Brush.linearGradient(
        listOf(Color(0xFFFFF7CC), Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFE57F))
    )
    val goldText = Brush.linearGradient(
        listOf(Color(0xFFFFFAD1), Color(0xFFFFD700), Color(0xFFFFA500))
    )
    val obsidianBg = Brush.horizontalGradient(
        listOf(Color(0xFF040508), Color(0xFF12100A), Color(0xFF050609))
    )

    Column {
        Text(
            "NAME",
            color = AccentGold,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(obsidianBg)
                .border(1.4.dp, goldBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(goldBorder)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = name,
                style = TextStyle(
                    brush = goldText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.6.sp
                ),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFD97706))
                        )
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isPro) "PRO HUNTER" else "HUNTER",
                    color = Color(0xFF06070A),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun IdentityField(label: String, value: String, accent: Boolean = false) {
    Column {
        Text(label, color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Text(
            value.ifBlank { "—" },
            color = if (accent) PrimaryManaBlue else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun RankCard(level: Int, streak: Int, onClick: () -> Unit) {
    val tier = HunterRankSystem.forLevel(level)
    val isLight = ThemeState.isLight
    val cardBorder = Brush.linearGradient(
        listOf(tier.accent, tier.primary, tier.secondary)
    )
    val badgeTextCol = if (isLight) tier.primary else tier.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isLight) {
                    Brush.horizontalGradient(
                        listOf(
                            DarkCard,
                            tier.primary.copy(alpha = 0.08f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            tier.primary.copy(alpha = if (tier.isPremiumFrame) 0.22f else 0.10f),
                            tier.frameBg.copy(alpha = 0.9f),
                            DarkCard.copy(alpha = 0.75f)
                        )
                    )
                }
            )
            .border(
                width = if (tier.isPremiumFrame) 1.5.dp else 1.dp,
                brush = cardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HunterRankBadge(tier = tier, size = 72.dp, animate = true)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "LEVEL $level",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(tier.primary.copy(alpha = 0.18f))
                        .border(0.8.dp, badgeTextCol, RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        tier.code,
                        color = badgeTextCol,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                tier.title,
                color = badgeTextCol,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Tap to view all 10 Hunter Ranks ›",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("STREAK", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$streak",
                    color = GoldWarning,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.width(4.dp))
                Text("🔥", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun BarsCard(hp: Int, ep: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.90f))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        BarRow(
            label = "HEALTH",
            value = "$hp / 100",
            progress = hp / 100f,
            colorStart = Color(0xFF00B33C),
            colorEnd = SuccessGreen
        )
        Spacer(Modifier.height(14.dp))
        BarRow(
            label = "ENERGY",
            value = "$ep / ${GamificationEngine.EP_PER_LEVEL} EP",
            progress = ep.toFloat() / GamificationEngine.EP_PER_LEVEL,
            colorStart = PrimaryManaBlue,
            colorEnd = PrimaryManaBlue
        )
    }
}

@Composable
private fun BarRow(
    label: String,
    value: String,
    progress: Float,
    colorStart: Color,
    colorEnd: Color
) {
    val smooth by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "smooth_$label"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                color = colorEnd,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                color = colorEnd,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(smooth)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(listOf(colorStart, colorEnd))
                    )
            )
        }
    }
}

@Composable
private fun SmallStatBox(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard.copy(alpha = 0.90f))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ActivityRow(event: EventEntity) {
    val headerAccent = if (ThemeState.isLight) PrimaryManaBlue else AccentCyan
    val (icon, color, title) = when (event.type) {
        EventType.OCCURRENCE_COMPLETED -> Triple(Icons.Rounded.CheckCircle, SuccessGreen, "Quest Completed")
        EventType.OCCURRENCE_MISSED -> Triple(Icons.Rounded.Warning, DangerPenaltyRed, "Quest Missed")
        EventType.PENALTY_APPLIED -> Triple(Icons.Rounded.Warning, DangerPenaltyRed, "Penalty Applied")
        EventType.RECOVERY_COMPLETED -> Triple(Icons.Rounded.ArrowUpward, GoldWarning, "Recovery Completed")
        EventType.LEVEL_UP -> Triple(Icons.Rounded.ArrowUpward, headerAccent, "Level Up!")
    }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard.copy(alpha = 0.90f))
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f))
                .border(1.dp, color.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (event.note.isNotBlank()) {
                Text(
                    event.note,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        Text(
            dateFormat.format(Date(event.timestamp)),
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}
