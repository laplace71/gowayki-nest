package com.gowayki.nesh.app.features.map.data

import gowaykinesh.shared.generated.resources.Res
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Descripción del paradero a partir del dataset local de lugares de referencia
 * (mismo que usa el panel web). Sin red: funciona offline.
 */
object StopDescriptions {
    private data class Lugar(
        val nombre: String,
        val detalle: String,
        val lat: Double,
        val lng: Double,
    )

    private val mutex = Mutex()
    private var lugares: List<Lugar> = emptyList()
    private var calles: Map<String, String> = emptyMap()
    private val cache = mutableMapOf<String, String>()

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun cargar(): List<Lugar> = mutex.withLock {
        if (lugares.isNotEmpty()) return@withLock lugares
        val texto = runCatching {
            Res.readBytes("files/lugares_referencia.json").decodeToString()
        }.getOrDefault("[]")
        lugares = runCatching { Json.parseToJsonElement(texto).jsonArray.mapNotNull(::toLugar) }
            .getOrDefault(emptyList())
        lugares
    }

    private fun toLugar(element: kotlinx.serialization.json.JsonElement): Lugar? = runCatching {
        val o = element.jsonObject
        Lugar(
            nombre = o["nombre"]?.jsonPrimitive?.content.orEmpty(),
            detalle = o["detalle"]?.jsonPrimitive?.content.orEmpty(),
            lat = o["lat"]!!.jsonPrimitive.double,
            lng = o["lng"]!!.jsonPrimitive.double,
        )
    }.getOrNull()

    private suspend fun cargarCalles(): Map<String, String> = mutex.withLock {
        if (calles.isNotEmpty()) return@withLock calles
        val texto = runCatching {
            Res.readBytes("files/paraderos_descripciones.json").decodeToString()
        }.getOrDefault("{}")
        calles = runCatching {
            Json.parseToJsonElement(texto).jsonObject.mapValues { it.value.jsonPrimitive.content }
        }.getOrDefault(emptyMap())
        calles
    }

    /**
     * Misma clave con la que se generó el JSON: 5 decimales con signo,
     * p. ej. "-16.42415|-71.55563". Antes se usaba el entero "-1642415|…"
     * y NUNCA coincidía, así que todos los paraderos caían al respaldo.
     */
    private fun parte5(valor: Double): String {
        val escalado = kotlin.math.round(valor * 100000.0).toLong()
        val signo = if (escalado < 0) "-" else ""
        val abs = kotlin.math.abs(escalado)
        return signo + (abs / 100000).toString() + "." +
            (abs % 100000).toString().padStart(5, '0')
    }

    private fun claveDe(lat: Double, lng: Double): String =
        "${parte5(lat)}|${parte5(lng)}"

    /** Devuelve la calle del paradero, o "Cerca de <lugar>" como respaldo. */
    suspend fun describe(lat: Double, lng: Double, defecto: String): String {
        val clave = claveDe(lat, lng)
        cache[clave]?.let { return it }
        val calle = cargarCalles()[clave]
        if (!calle.isNullOrBlank()) {
            cache[clave] = calle
            return calle
        }
        val cercano = cargar()
            .filter { distanciaMetros(lat, lng, it.lat, it.lng) <= 350.0 }
            .minByOrNull { distanciaMetros(lat, lng, it.lat, it.lng) }
        val texto = when {
            cercano == null -> defecto
            cercano.detalle.isNotBlank() -> "Cerca de ${cercano.nombre} · ${cercano.detalle}"
            else -> "Cerca de ${cercano.nombre}"
        }
        // No se cachea un texto vacío: al tocar el paradero se pasa un mensaje
        // de respaldo distinto y debía poder resolverlo de nuevo.
        if (texto.isNotBlank()) cache[clave] = texto
        return texto
    }

    private fun distanciaMetros(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val radio = 6371000.0
        val p1 = aLat * kotlin.math.PI / 180.0
        val p2 = bLat * kotlin.math.PI / 180.0
        val dLat = p2 - p1
        val dLng = (bLng - aLng) * kotlin.math.PI / 180.0
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(p1) * cos(p2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * radio * asin(minOf(1.0, sqrt(h)))
    }
}
