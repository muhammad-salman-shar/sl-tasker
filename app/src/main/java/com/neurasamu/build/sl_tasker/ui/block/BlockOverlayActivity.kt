package com.neurasamu.build.sl_tasker.ui.block

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neurasamu.build.sl_tasker.ui.MainActivity
import com.neurasamu.build.sl_tasker.ui.theme.SoloLevelingTheme

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val blockedPkg = intent.getStringExtra("blocked_pkg")

        setContent {
            SoloLevelingTheme {
                BlockOverlayScreen(
                    blockedPackage = blockedPkg,
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
        // Ignore back — user must go through Tasker.
    }
}
