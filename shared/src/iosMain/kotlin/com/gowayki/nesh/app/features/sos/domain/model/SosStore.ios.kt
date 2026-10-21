// Actual iOS — NSUserDefaults (sin librerías nuevas).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.features.sos.domain.model

import platform.Foundation.NSUserDefaults

actual object SosStore {
    private const val KEY = "wayki_sos_points"
    // Separadores como código (31/30) para no meter caracteres invisibles.
    private val SEP = 31.toChar().toString()
    private val ROW = 30.toChar().toString()

    actual suspend fun save(points: List<SosStoredPoint>) {
        runCatching {
            val raw = points.joinToString(ROW) {
                listOf(it.id, it.lat.toString(), it.lon.toString(), it.title, it.timeMillis.toString())
                    .joinToString(SEP)
            }
            NSUserDefaults.standardUserDefaults.setObject(raw, KEY)
        }
    }

    actual suspend fun load(): List<SosStoredPoint> {
        return runCatching {
            val raw = NSUserDefaults.standardUserDefaults.stringForKey(KEY) ?: return emptyList()
            if (raw.isBlank()) return emptyList()
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
