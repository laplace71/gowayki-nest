package com.gowayki.nesh.app.features.map.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object RouteSelectionStore {
    private const val FILE = "wayki_route_prefs"
    private const val KEY = "selected_route_id"
    private const val KEY_NAME = "selected_route_name"
    private const val KEY_IDA_ROUTE_ID = "selected_ida_route_id"
    private const val KEY_RETURN_ROUTE_ID = "selected_return_route_id"
    private const val KEY_DIRECTION = "selected_route_direction"
    private const val ACTION_REFRESH_WIDGET = "com.gowayki.nesh.action.REFRESH_RECORRIDO_WIDGET"

    actual suspend fun save(
        routeId: String,
        routeName: String,
        idaRouteId: String?,
        returnRouteId: String?,
        direction: String
    ) {
        val ctx = AndroidSosContext.appContext ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY, routeId)
                    .putString(KEY_NAME, routeName)
                    .putString(KEY_IDA_ROUTE_ID, idaRouteId)
                    .putString(KEY_RETURN_ROUTE_ID, returnRouteId)
                    .putString(KEY_DIRECTION, direction)
                    .commit()

                // Refresco directo (más fiable que solo broadcast)
                runCatching {
                    val mgr = AppWidgetManager.getInstance(ctx)
                    val cn = ComponentName(ctx, "com.gowayki.nesh.widget.RecorridoWidgetProvider")
                    val ids = mgr.getAppWidgetIds(cn)
                    if (ids.isNotEmpty()) {
                        mgr.notifyAppWidgetViewDataChanged(ids, ctx.resources.getIdentifier("widget_list", "id", ctx.packageName))
                        val update = Intent(ACTION_REFRESH_WIDGET).apply { setPackage(ctx.packageName) }
                        ctx.sendBroadcast(update)
                    } else {
                        val refresh = Intent(ACTION_REFRESH_WIDGET).apply { setPackage(ctx.packageName) }
                        ctx.sendBroadcast(refresh)
                    }
                }
            }
        }
    }

    actual suspend fun load(): String? {
        val ctx = AndroidSosContext.appContext ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .getString(KEY, null)
            }.getOrNull()
        }
    }

    actual suspend fun clear() {
        val ctx = AndroidSosContext.appContext ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY)
                    .remove(KEY_NAME)
                    .remove(KEY_IDA_ROUTE_ID)
                    .remove(KEY_RETURN_ROUTE_ID)
                    .remove(KEY_DIRECTION)
                    .commit()
            }
        }
    }
}
