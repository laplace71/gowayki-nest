// Actual Android — SharedPreferences (sin librerías nuevas).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object SosStore {
    private const val FILE = "sos_store"
    private const val KEY = "points"
    private const val SEP = "\u001F"
    private const val ROW = "\u001E"

    actual suspend fun save(points: List<SosStoredPoint>) {
        val ctx = AndroidSosContext.appContext ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val raw = points.joinToString(ROW) {
                    listOf(it.id, it.lat.toString(), it.lon.toString(), it.title, it.timeMillis.toString())
                        .joinToString(SEP)
                }
                ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .edit().putString(KEY, raw).apply()
            }
        }
    }

    actual suspend fun load(): List<SosStoredPoint> {
        val ctx = AndroidSosContext.appContext ?: return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val raw = ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                    .getString(KEY, null) ?: return@runCatching emptyList()
                if (raw.isBlank()) return@runCatching emptyList<SosStoredPoint>()
                raw.split(ROW).mapNotNull { row ->
                    val p = row.split(SEP)
                    if (p.size != 5) return@mapNotNull null
                    SosStoredPoint(
                        id = p[0],
                        lat = p[1].toDoubleOrNull() ?: return@mapNotNull null,
                        lon = p[2].toDoubleOrNull() ?: return@mapNotNull null,
                        title = p[3],
                        timeMillis = p[4].toLongOrNull() ?: return@mapNotNull null,
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
