package com.neurasamu.build.sl_tasker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.neurasamu.build.sl_tasker.R
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val stats = db.playerStatsDao().getPlayerStats()

                    val views = RemoteViews(context.packageName, R.layout.widget_stats)
                    views.setTextViewText(
                        R.id.w_level,
                        "LV ${stats?.level ?: 1}"
                    )
                    views.setTextViewText(
                        R.id.w_hp,
                        "HP ${stats?.health ?: 100}"
                    )
                    views.setTextViewText(
                        R.id.w_ep,
                        "EP ${stats?.ep ?: 0}"
                    )
                    views.setTextViewText(
                        R.id.w_streak,
                        "🔥 ${stats?.streak ?: 0}"
                    )
                    views.setTextViewText(
                        R.id.w_completed,
                        "${stats?.totalCompleted ?: 0} cleared"
                    )

                    val launchIntent = Intent(context, MainActivity::class.java)
                    val pi = PendingIntent.getActivity(
                        context, 0, launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pi)

                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (_: Throwable) {}
            }
        }
    }
}
