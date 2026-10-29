package com.neurasamu.build.sl_tasker.ui.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.neurasamu.build.sl_tasker.data.player.PlayerProfileStore
import com.neurasamu.build.sl_tasker.ui.MainActivity
import kotlinx.coroutines.launch

class IntroActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var fallbackView: TextView
    private var bridgeCalled = false

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive full-screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this)

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

            addJavascriptInterface(Bridge(), "AndroidBridge")

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        showFallback("WebView failed. Tap to skip intro.")
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }
            loadUrl("file:///android_asset/intro.html")
        }

        fallbackView = TextView(this).apply {
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
            visibility = View.GONE
            setText("Intro failed to load.\n\nTap to skip and enter the app.")
            setOnClickListener { skipIntro() }
        }

        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(fallbackView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(root)

        // If bridge doesn't fire within 30s, show fallback (user might be stuck)
        lifecycleScope.launch {
            kotlinx.coroutines.delay(30_000L)
            if (!bridgeCalled && fallbackView.visibility != View.VISIBLE) {
                // Don't force fallback, just enable skip mode via tap
                // Softer: do nothing. User can still complete.
            }
        }
    }

    private fun showFallback(msg: String) {
        runOnUiThread {
            fallbackView.text = msg
            fallbackView.visibility = View.VISIBLE
        }
    }

    private fun skipIntro() {
        if (bridgeCalled) return
        bridgeCalled = true
        lifecycleScope.launch {
            PlayerProfileStore(applicationContext).save(
                name = "Player",
                gender = "unknown",
                age = 18,
                playerId = "player_" + System.currentTimeMillis().toString().takeLast(4)
            )
            enterApp()
        }
    }

    private fun enterApp() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    inner class Bridge {
        @JavascriptInterface
        fun onIntroComplete(name: String, gender: String, age: Int, playerId: String) {
            if (bridgeCalled) return
            bridgeCalled = true

            val safeName = if (name.isBlank()) "Player" else name.trim()
            val safeGender = if (gender.isBlank()) "unknown" else gender.trim()
            val safeAge = if (age in 12..90) age else 18
            val safeId = if (playerId.isBlank()) "player_" + System.currentTimeMillis().toString().takeLast(4) else playerId.trim()

            lifecycleScope.launch {
                PlayerProfileStore(applicationContext).save(safeName, safeGender, safeAge, safeId)
                runOnUiThread { enterApp() }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back — intro must complete
    }
}
