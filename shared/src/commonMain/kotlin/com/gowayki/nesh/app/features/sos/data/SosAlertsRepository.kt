package com.gowayki.nesh.app.features.sos.data

import com.gowayki.nesh.app.features.sos.domain.model.SosAlert
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
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

    suspend fun closeAlert(alertId: String) {
        val response = client.delete("$baseUrl/api/sos/alerts/$alertId")
        check(response.status.value in 200..299) {
            "El servidor rechazó el cierre de la alerta (${response.status.value})."
        }
    }

    fun close() = client.close()
}
