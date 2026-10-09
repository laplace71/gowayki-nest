package com.gowayki.nesh.app.features.sos.data

import com.gowayki.nesh.app.features.sos.domain.model.SosAlert
import com.gowayki.nesh.app.features.reports.domain.model.OsmLatLng
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SosAlertsRepository(private val baseUrl: String) {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 4_000
            socketTimeoutMillis = 8_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun activeAlerts(): List<SosAlert> =
        client.get("$baseUrl/api/sos/alerts?status=ACTIVE").body()

    suspend fun publishPhoneLocation(
        token: String,
        deviceId: String,
        location: OsmLatLng,
        capturedAtEpochMs: Long,
        accuracyMeters: Float,
    ) {
        val response = client.post("$baseUrl/api/sos/locations") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                PhoneLocationUpdate(
                    deviceId = deviceId,
                    latitude = location.lat,
                    longitude = location.lon,
                    capturedAtEpochMs = capturedAtEpochMs,
                    accuracyMeters = accuracyMeters,
                ),
            )
        }
        if (response.status.value !in 200..299) {
            val serverMessage = response.bodyAsText().takeIf(String::isNotBlank)
            error(
                serverMessage
                    ?: "No se pudo compartir la ubicación con el servidor (${response.status.value}).",
            )
        }
    }

    suspend fun closeAlert(alertId: String) {
        val response = client.delete("$baseUrl/api/sos/alerts/$alertId")
        check(response.status.value in 200..299) {
            "El servidor rechazó el cierre de la alerta (${response.status.value})."
        }
    }

    fun close() = client.close()
}

@Serializable
private data class PhoneLocationUpdate(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val capturedAtEpochMs: Long,
    val accuracyMeters: Float,
)
