package com.gowayki.nesh.widget

import android.content.Context
import org.json.JSONObject

data class RecorridoStopItem(
    val name: String,
    val district: String?,
    val isHeader: Boolean = false
)

data class RecorridoSelection(
    val routeId: String,
    val displayName: String,
    val directionLabel: String,
    val stopCount: Int
)

object RecorridoWidgetData {
    private const val PREFS = "wayki_route_prefs"
    private const val KEY_ROUTE_ID = "selected_route_id"
    private const val KEY_ROUTE_NAME = "selected_route_name"
    private const val KEY_IDA_ROUTE_ID = "selected_ida_route_id"
    private const val KEY_RETURN_ROUTE_ID = "selected_return_route_id"
    private const val KEY_DIRECTION = "selected_route_direction"
    private const val ASSET_PATH = "composeResources/gowaykinesh.shared.generated.resources/files/sit_route_stops.json"

    fun loadSelection(context: Context): RecorridoSelection? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val routeId = prefs.getString(KEY_ROUTE_ID, null) ?: return null
        val storedName = prefs.getString(KEY_ROUTE_NAME, null)?.takeIf { it.isNotBlank() }
        val idaRouteId = prefs.getString(KEY_IDA_ROUTE_ID, null)
        val returnRouteId = prefs.getString(KEY_RETURN_ROUTE_ID, null)
        val direction = prefs.getString(KEY_DIRECTION, null) ?: "IDA"
        val stops = buildStopItems(context, idaRouteId, returnRouteId, direction)
        val displayName = storedName ?: routeDisplayName(routeId)
        val hasReturn = !returnRouteId.isNullOrBlank() && readStopsForRoute(context, returnRouteId).isNotEmpty()
        val label = when {
            hasReturn && readStopsForRoute(context, idaRouteId ?: "").isNotEmpty() -> "IDA + VUELTA"
            direction == "RETORNO" && hasReturn -> "VUELTA"
            else -> "IDA"
        }
        return RecorridoSelection(
            routeId = routeId,
            displayName = displayName,
            directionLabel = label,
            stopCount = stops.count { !it.isHeader }
        )
    }

    fun loadStops(context: Context): List<RecorridoStopItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val idaRouteId = prefs.getString(KEY_IDA_ROUTE_ID, null)
        val returnRouteId = prefs.getString(KEY_RETURN_ROUTE_ID, null)
        val direction = prefs.getString(KEY_DIRECTION, null) ?: "IDA"
        // Si no hay routeIds nuevos (selección antigua), intenta usar el id guardado como routeId directo
        if (idaRouteId.isNullOrBlank() && returnRouteId.isNullOrBlank()) {
            val legacy = prefs.getString(KEY_ROUTE_ID, null) ?: return emptyList()
            return readStopsForRoute(context, legacy).take(30)
        }
        return buildStopItems(context, idaRouteId, returnRouteId, direction).take(40)
    }

    private fun buildStopItems(
        context: Context,
        idaRouteId: String?,
        returnRouteId: String?,
        direction: String
    ): List<RecorridoStopItem> {
        val ida = readStopsForRoute(context, idaRouteId)
        val ret = readStopsForRoute(context, returnRouteId)

        // Muestra ida y vuelta cuando existen para que el widget refleje ambos recorridos.
        if (ida.isNotEmpty() && ret.isNotEmpty()) {
            val out = ArrayList<RecorridoStopItem>(ida.size + ret.size + 2)
            out.add(RecorridoStopItem(name = "Ida", district = "${ida.size} paraderos", isHeader = true))
            out.addAll(ida.take(20))
            out.add(RecorridoStopItem(name = "Vuelta", district = "${ret.size} paraderos", isHeader = true))
            out.addAll(ret.take(20))
            return out
        }
        if (direction == "RETORNO" && ret.isNotEmpty()) return ret
        if (ida.isNotEmpty()) return ida
        return ret
    }

    private fun readStopsForRoute(context: Context, routeId: String?): List<RecorridoStopItem> {
        if (routeId.isNullOrBlank()) return emptyList()
        return try {
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            if (!root.has(routeId)) return emptyList()
            val arr = root.getJSONArray(routeId)
            val out = ArrayList<RecorridoStopItem>(arr.length())
            val seen = HashSet<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isBlank() || name.equals("Innominada", ignoreCase = true)) continue
                val district = obj.optString("district", "").trim().ifBlank { null }
                val key = name.lowercase() + "|" + (district ?: "").lowercase()
                if (!seen.add(key)) continue
                out.add(
                    RecorridoStopItem(
                        name = name,
                        district = district?.lowercase()?.split(" ")?.joinToString(" ") { w ->
                            w.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
                        }
                    )
                )
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun routeDisplayName(routeId: String): String {
        return when {
            routeId.startsWith("SB-") -> "Ruta $routeId"
            routeId.matches(Regex("\\d+")) -> "Ruta SIT $routeId"
            else -> "Línea $routeId"
        }
    }
}
