package com.neurasamu.build.sl_tasker.ui.block

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.block.BlockState
import com.neurasamu.build.sl_tasker.ui.MainActivity
import com.neurasamu.build.sl_tasker.ui.theme.SoloLevelingTheme
import kotlinx.coroutines.launch

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val blockedPkg = intent.getStringExtra("blocked_pkg")

        setContent {
            val blockPrefs = remember { BlockPrefs(applicationContext) }
            val blockState by blockPrefs.state.collectAsState(initial = BlockState())
            val scope = rememberCoroutineScope()

            SoloLevelingTheme {
                BlockOverlayScreen(
                    blockedPackage = blockedPkg,
                    blockState = blockState,
                    onUseEarnedScroll = {
                        scope.launch {
                            val ok = blockPrefs.activateScrollUnlock(minutes = 10, consumeEarnedQuota = true)
                            if (ok) {
                                Toast.makeText(
                                    applicationContext,
                                    "Unlocked for 10 min using Walk-to-Earn quota!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Walk 500 real steps to earn 10 min scroll quota.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onSealVerified = {
                        scope.launch {
                            blockPrefs.activateScrollUnlock(minutes = 10, consumeEarnedQuota = false)
                            Toast.makeText(
                                applicationContext,
                                "Physical Seal verified! 10 min unlock granted.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    },
                    onOpenTasker = {
                        val i = Intent(this, MainActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(i)
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Ignore back — user must verify physical seal, spend earned walk quota, or return to SL Tasker.
    }
}
