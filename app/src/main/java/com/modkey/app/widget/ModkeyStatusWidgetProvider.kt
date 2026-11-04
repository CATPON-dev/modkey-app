package com.modkey.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.modkey.app.MainActivity
import com.modkey.app.ModkeyApp
import com.modkey.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ModkeyStatusWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_SOFT = "com.modkey.app.widget.ACTION_TOGGLE_SOFT"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshWidgets(context, appWidgetManager, appWidgetIds, toggleStatus = false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_SOFT) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ModkeyStatusWidgetProvider::class.java))
            refreshWidgets(context, manager, ids, toggleStatus = true)
        }
    }

    private fun refreshWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        toggleStatus: Boolean
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = ModkeyApp.instance
                val activeAccount = app.accountManager.getActiveAccount()
                val views = RemoteViews(context.packageName, R.layout.widget_modkey_status)

                val openAppIntent = Intent(context, MainActivity::class.java)
                val openAppPending = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openAppPending)

                val toggleIntent = Intent(context, ModkeyStatusWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_SOFT
                }
                val togglePending = PendingIntent.getBroadcast(
                    context,
                    1,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_toggle_btn, togglePending)

                if (activeAccount == null) {
                    views.setTextViewText(R.id.widget_soft_name, "Modkey")
                    views.setTextViewText(R.id.widget_status_text, "Войдите в аккаунт")
                } else {
                    val softs = app.apiClient.getSofts(activeAccount.apiKey)
                    val firstSoft = softs.firstOrNull()
                    if (firstSoft != null) {
                        val currentSoft = if (toggleStatus) {
                            val nextStatus = if (firstSoft.status == 1) 0 else 1
                            app.apiClient.editSoftStatus(
                                apiKey = activeAccount.apiKey,
                                softId = firstSoft.id,
                                status = nextStatus,
                                reason = if (nextStatus == 0) "Техработы (с виджета)" else null
                            )
                        } else {
                            firstSoft
                        }
                        val stats = app.apiClient.getSoftStats(activeAccount.apiKey, currentSoft.id)
                        val isOnline = currentSoft.status == 1
                        val activeKeys = stats.keys?.active ?: 0
                        val totalKeys = stats.keys?.total ?: 0

                        views.setTextViewText(R.id.widget_soft_name, "${currentSoft.name} (#${currentSoft.id})")
                        views.setTextViewText(
                            R.id.widget_status_text,
                            if (isOnline) "● В сети (Online) • Активных: $activeKeys/$totalKeys"
                            else "● Техработы (Offline) • Ключей: $totalKeys"
                        )
                        views.setTextColor(
                            R.id.widget_status_text,
                            if (isOnline) Color.parseColor("#81C784") else Color.parseColor("#E57373")
                        )
                        views.setTextViewText(
                            R.id.widget_toggle_btn,
                            if (isOnline) "В техработы" else "Включить"
                        )
                    }
                }

                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
