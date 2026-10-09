package com.gowayki.nesh.sos.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors

@Serializable
data class SosAlertRequest(
    val eventId: String,
    val deviceId: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val occurredAt: String,
    val simulated: Boolean = false,
)

@Serializable
data class PhoneLocationRequest(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val capturedAtEpochMs: Long,
    val accuracyMeters: Float,
)

@Serializable
data class SosAlert(
    val id: String,
    val eventId: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val occurredAt: String,
    val status: String = "ACTIVE",
    val simulated: Boolean = false,
)

@Serializable
private data class ApiError(val error: String)

private class AlertStore {
    private val lock = Any()
    private val alerts = mutableListOf<SosAlert>()
    private val lastAcceptedAtNanos = mutableMapOf<String, Long>()
    private val locations = mutableMapOf<String, StoredPhoneLocation>()

    fun byStatus(status: String): List<SosAlert> = synchronized(lock) {
        alerts.filter { it.status == status }
    }

    fun updateLocation(request: PhoneLocationRequest): Boolean = synchronized(lock) {
        val now = System.currentTimeMillis()
        if (request.capturedAtEpochMs < now - MAX_LOCATION_AGE_MS ||
            request.capturedAtEpochMs > now + MAX_FUTURE_LOCATION_SKEW_MS
        ) return false
        locations[request.deviceId] = StoredPhoneLocation(
            latitude = request.latitude,
            longitude = request.longitude,
            accuracyMeters = request.accuracyMeters,
            capturedAtEpochMs = request.capturedAtEpochMs,
            receivedAtEpochMs = now,
        )
        true
    }

    fun create(request: SosAlertRequest, allowSimulated: Boolean): CreateResult = synchronized(lock) {
        alerts.firstOrNull { it.deviceId == request.deviceId && it.eventId == request.eventId }
            ?.let { return CreateResult(it, created = false) }
        val phoneLocation = locations[request.deviceId]?.takeIf {
            System.currentTimeMillis() - it.receivedAtEpochMs <= MAX_LOCATION_AGE_MS &&
                it.accuracyMeters <= MAX_LOCATION_ACCURACY_METERS
        }
        val simulatedLocation = if (
            phoneLocation == null && allowSimulated && request.simulated &&
            request.latitude != null && request.longitude != null
        ) {
            request.latitude to request.longitude
        } else {
            null
        }
        val position = phoneLocation?.let { it.latitude to it.longitude } ?: simulatedLocation
        if (position == null) return CreateResult(alert = null, created = false, missingLocation = true)
        val now = System.nanoTime()
        val previous = lastAcceptedAtNanos[request.deviceId]
        if (previous != null && now - previous < BUTTON_DEBOUNCE_NANOS) {
            return CreateResult(alert = null, created = false, debounced = true)
        }
        val alert = SosAlert(
            id = UUID.randomUUID().toString(),
            eventId = request.eventId,
            deviceId = request.deviceId,
            latitude = position.first,
            longitude = position.second,
            occurredAt = request.occurredAt,
            simulated = phoneLocation == null,
        )
        alerts.add(alert)
        lastAcceptedAtNanos[request.deviceId] = now
        CreateResult(alert, created = true)
    }

    fun close(id: String): SosAlert? = synchronized(lock) {
        val index = alerts.indexOfFirst { it.id == id }
        if (index < 0) return null
        alerts[index] = alerts[index].copy(status = "CLOSED")
        alerts[index]
    }
}

private data class StoredPhoneLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtEpochMs: Long,
    val receivedAtEpochMs: Long,
)

private data class CreateResult(
    val alert: SosAlert?,
    val created: Boolean,
    val debounced: Boolean = false,
    val missingLocation: Boolean = false,
)

private val json = Json { ignoreUnknownKeys = false }
private val eventIdPattern = Regex("[A-Za-z0-9_-]{8,80}")
private val deviceIdPattern = Regex("[A-Za-z0-9._-]{1,64}")

fun main() {
    val deviceTokens = loadDeviceTokens()
    val mobileLocationToken = loadSecret("SOS_MOBILE_LOCATION_TOKEN")
    val allowSimulated = System.getenv("SOS_ALLOW_SIMULATED")?.equals("true", ignoreCase = true) == true
    val port = (System.getenv("SOS_SERVER_PORT") ?: "8080").toIntOrNull()
        ?.takeIf { it in 1..65535 }
        ?: error("SOS_SERVER_PORT debe ser un puerto TCP válido.")
    val bindAddress = System.getenv("SOS_SERVER_BIND") ?: "0.0.0.0"

    val store = AlertStore()
    val server = HttpServer.create(InetSocketAddress(bindAddress, port), 0)
    server.executor = Executors.newFixedThreadPool(4)
    val handler: (HttpExchange) -> Unit = { exchange ->
        handleRequest(exchange, store, deviceTokens, mobileLocationToken, allowSimulated)
    }
    server.createContext("/api/sos/alerts", handler)
    server.createContext("/api/sos/locations", handler)
    server.start()
    println("Servicio SOS escuchando en $bindAddress:$port (HTTP; utilícese solo en red local de desarrollo).")
}

private fun handleRequest(
    exchange: HttpExchange,
    store: AlertStore,
    deviceTokens: Map<String, String>,
    mobileLocationToken: String,
    allowSimulated: Boolean,
) {
    try {
        val path = URI.create(exchange.requestURI.toString()).path
        when {
            exchange.requestMethod == "POST" && path == "/api/sos/alerts" ->
                receiveAlert(exchange, store, deviceTokens, allowSimulated)
            exchange.requestMethod == "POST" && path == "/api/sos/locations" ->
                receivePhoneLocation(exchange, store, mobileLocationToken)
            exchange.requestMethod == "GET" && path == "/api/sos/alerts" ->
                listAlerts(exchange, store)
            exchange.requestMethod == "DELETE" && path.startsWith("/api/sos/alerts/") ->
                closeAlert(exchange, store, path.substringAfterLast('/'))
            else -> respond(exchange, 404, ApiError("Ruta o método no encontrado."))
        }
    } catch (error: Exception) {
        System.err.println("Error atendiendo petición SOS: ${error.message}")
        respond(exchange, 500, ApiError("Error interno procesando la solicitud."))
    } finally {
        exchange.close()
    }
}

private fun receiveAlert(
    exchange: HttpExchange,
    store: AlertStore,
    deviceTokens: Map<String, String>,
    allowSimulated: Boolean,
) {
    if (!isJson(exchange)) {
        respond(exchange, 415, ApiError("Se requiere Content-Type application/json."))
        return
    }
    val payload = exchange.requestBody.use { it.readNBytes(MAX_REQUEST_BYTES + 1) }
    if (payload.size > MAX_REQUEST_BYTES) {
        respond(exchange, 413, ApiError("Solicitud demasiado grande."))
        return
    }
    val request = try {
        json.decodeFromString<SosAlertRequest>(payload.decodeToString())
    } catch (_: SerializationException) {
        respond(exchange, 400, ApiError("JSON inválido o faltan campos obligatorios."))
        return
    }
    val expectedToken = deviceTokens[request.deviceId]
    if (expectedToken == null || !authorized(exchange, expectedToken)) {
        respond(exchange, 401, ApiError("Autenticación de dispositivo inválida."))
        return
    }
    if (!validAlert(request)) {
        respond(exchange, 400, ApiError("Identificador, coordenadas o fecha de activación inválidos."))
        return
    }
    val result = store.create(request, allowSimulated)
    if (result.missingLocation) {
        respond(exchange, 409, ApiError("No hay una ubicación GPS reciente y precisa para este dispositivo."))
        return
    }
    if (result.debounced) {
        exchange.responseHeaders.set("Retry-After", BUTTON_DEBOUNCE_SECONDS.toString())
        respond(exchange, 429, ApiError("Pulsación repetida; espera antes de generar otra alerta."))
        return
    }
    respond(exchange, if (result.created) 201 else 200, requireNotNull(result.alert))
}

private fun receivePhoneLocation(exchange: HttpExchange, store: AlertStore, expectedToken: String) {
    if (!isPrivateNetwork(exchange.remoteAddress.address.hostAddress)) {
        respond(exchange, 403, ApiError("El GPS móvil solo puede compartirse desde una red privada."))
        return
    }
    if (!authorized(exchange, expectedToken)) {
        respond(exchange, 401, ApiError("Token de ubicación móvil inválido."))
        return
    }
    if (!isJson(exchange)) {
        respond(exchange, 415, ApiError("Se requiere Content-Type application/json."))
        return
    }
    val payload = exchange.requestBody.use { it.readNBytes(MAX_REQUEST_BYTES + 1) }
    if (payload.size > MAX_REQUEST_BYTES) {
        respond(exchange, 413, ApiError("Solicitud demasiado grande."))
        return
    }
    val request = try {
        json.decodeFromString<PhoneLocationRequest>(payload.decodeToString())
    } catch (_: SerializationException) {
        respond(exchange, 400, ApiError("JSON de ubicación inválido o incompleto."))
        return
    }
    if (!validPhoneLocation(request)) {
        respond(exchange, 400, ApiError("ID, coordenadas, precisión o fecha GPS inválidos."))
        return
    }
    if (!store.updateLocation(request)) {
        respond(exchange, 400, ApiError("La ubicación GPS está vencida o tiene una fecha futura."))
        return
    }
    respond(exchange, 202, ApiError("Ubicación actualizada."))
}

private fun listAlerts(exchange: HttpExchange, store: AlertStore) {
    if (!isPrivateNetwork(exchange.remoteAddress.address.hostAddress)) {
        respond(exchange, 403, ApiError("Las alertas solo están disponibles desde una red privada."))
        return
    }
    val status = exchange.requestURI.rawQuery
        ?.split('&')
        ?.firstOrNull { it.startsWith("status=") }
        ?.substringAfter('=')
        ?.uppercase()
        ?: "ACTIVE"
    if (status != "ACTIVE" && status != "CLOSED") {
        respond(exchange, 400, ApiError("status debe ser ACTIVE o CLOSED."))
        return
    }
    respond(exchange, 200, store.byStatus(status))
}

private fun closeAlert(exchange: HttpExchange, store: AlertStore, id: String) {
    if (!isPrivateNetwork(exchange.remoteAddress.address.hostAddress)) {
        respond(exchange, 403, ApiError("Las alertas solo pueden cerrarse desde una red privada."))
        return
    }
    if (id.isBlank() || id.contains('/')) {
        respond(exchange, 400, ApiError("Identificador de alerta inválido."))
        return
    }
    val alert = store.close(id)
    if (alert == null) respond(exchange, 404, ApiError("Alerta no encontrada."))
    else respond(exchange, 200, alert)
}

private fun validAlert(request: SosAlertRequest): Boolean {
    if (!eventIdPattern.matches(request.eventId) || !deviceIdPattern.matches(request.deviceId)) return false
    if ((request.latitude == null) != (request.longitude == null)) return false
    if (request.simulated && request.latitude == null) return false
    if (request.latitude != null && (!request.latitude.isFinite() || request.latitude !in -90.0..90.0)) return false
    if (request.longitude != null && (!request.longitude.isFinite() || request.longitude !in -180.0..180.0)) return false
    return runCatching { Instant.parse(request.occurredAt) }.isSuccess
}

private fun validPhoneLocation(request: PhoneLocationRequest): Boolean =
    deviceIdPattern.matches(request.deviceId) &&
        request.latitude.isFinite() && request.latitude in -90.0..90.0 &&
        request.longitude.isFinite() && request.longitude in -180.0..180.0 &&
        request.accuracyMeters.isFinite() &&
        request.accuracyMeters in 0f..MAX_LOCATION_ACCURACY_METERS

private fun authorized(exchange: HttpExchange, expectedToken: String): Boolean {
    val header = exchange.requestHeaders.getFirst("Authorization") ?: return false
    val provided = header.removePrefix("Bearer ").takeIf { header.startsWith("Bearer ") } ?: return false
    return MessageDigest.isEqual(provided.toByteArray(), expectedToken.toByteArray())
}

private fun loadDeviceTokens(): Map<String, String> {
    val configured = System.getenv("SOS_DEVICE_TOKENS")
        ?.takeIf(String::isNotBlank)
        ?: error("Define SOS_DEVICE_TOKENS con pares DEVICE_ID=token separados por coma.")
    val tokens = configured.split(',').associate { entry ->
        val separator = entry.indexOf('=')
        require(separator > 0 && separator < entry.lastIndex) {
            "SOS_DEVICE_TOKENS debe usar el formato DEVICE_ID=token."
        }
        val deviceId = entry.substring(0, separator).trim()
        val token = entry.substring(separator + 1).trim()
        require(deviceIdPattern.matches(deviceId) && token.length >= 32) {
            "Cada dispositivo debe tener un identificador válido y un token de al menos 32 caracteres."
        }
        deviceId to token
    }
    require(tokens.size == configured.split(',').size) {
        "SOS_DEVICE_TOKENS no puede repetir identificadores de dispositivo."
    }
    return tokens
}

private fun loadSecret(name: String): String =
    System.getenv(name)?.takeIf { it.length >= 32 }
        ?: error("Define $name con al menos 32 caracteres aleatorios antes de iniciar el servidor.")

private fun isJson(exchange: HttpExchange): Boolean =
    exchange.requestHeaders.getFirst("Content-Type")
        ?.substringBefore(';')
        ?.trim()
        ?.equals("application/json", ignoreCase = true) == true

private fun isPrivateNetwork(host: String): Boolean {
    if (host == "localhost" || host == "::1" || host.startsWith("127.")) return true
    val parts = host.split('.').mapNotNull(String::toIntOrNull)
    if (parts.size != 4 || parts.any { it !in 0..255 }) return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")
    return parts[0] == 10 ||
        (parts[0] == 172 && parts[1] in 16..31) ||
        (parts[0] == 192 && parts[1] == 168) ||
        (parts[0] == 169 && parts[1] == 254)
}

private inline fun <reified T> respond(exchange: HttpExchange, status: Int, body: T) {
    val response = json.encodeToString(body)
    val bytes = response.toByteArray()
    exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
    exchange.responseHeaders.set("Cache-Control", "no-store")
    exchange.sendResponseHeaders(status, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}

private const val MAX_REQUEST_BYTES = 8 * 1024
private const val BUTTON_DEBOUNCE_SECONDS = 2
private const val BUTTON_DEBOUNCE_NANOS = BUTTON_DEBOUNCE_SECONDS * 1_000_000_000L
private const val MAX_LOCATION_AGE_MS = 15_000L
private const val MAX_FUTURE_LOCATION_SKEW_MS = 30_000L
private const val MAX_LOCATION_ACCURACY_METERS = 100f
