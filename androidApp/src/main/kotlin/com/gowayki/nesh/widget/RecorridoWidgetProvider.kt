package com.gowayki.nesh.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.gowayki.nesh.MainActivity
import com.gowayki.nesh.R

class RecorridoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (ACTION_REFRESH == intent.action) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, RecorridoWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
                ids.forEach { updateAppWidget(context, mgr, it) }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.gowayki.nesh.action.REFRESH_RECORRIDO_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_recorrido)

            val selection = RecorridoWidgetData.loadSelection(context)
            if (selection == null) {
                views.setTextViewText(R.id.widget_title, "Tu recorrido")
                views.setTextViewText(R.id.widget_subtitle, "Selecciona una ruta en la app")
                views.setViewVisibility(R.id.widget_badge, View.GONE)
            } else {
                views.setTextViewText(R.id.widget_title, selection.displayName)
                views.setTextViewText(
                    R.id.widget_subtitle,
                    "${selection.stopCount} paraderos · ${selection.directionLabel}"
                )
                views.setViewVisibility(R.id.widget_badge, View.VISIBLE)
                views.setTextViewText(R.id.widget_badge, selection.directionLabel)
            }

            val serviceIntent = Intent(context, RecorridoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestRefresh(context: Context) {
            val intent = Intent(context, RecorridoWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
