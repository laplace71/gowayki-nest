package com.gowayki.nesh.core.network

import com.gowayki.nesh.core.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    val client: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    encodeDefaults = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                com.gowayki.nesh.core.session.AuthTokenState.authorizationHeader?.let { valor ->
                    header(HttpHeaders.Authorization, valor)
                }
            }
        }
    }

    val uploadClient: HttpClient by lazy {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            // El audio también va autenticado: el backend toma el emisor del token.
            defaultRequest {
                com.gowayki.nesh.core.session.AuthTokenState.authorizationHeader?.let { valor ->
                    header(HttpHeaders.Authorization, valor)
                }
            }
        }
    }

    val wsClient: HttpClient by lazy {
        HttpClient {
            install(io.ktor.client.plugins.websocket.WebSockets) {
                pingIntervalMillis = 20_000
            }
        }
    }

    val baseUrl: String
        get() = AppConfig.BACKEND_URL

    val wsChatUrl: String
        get() = AppConfig.BACKEND_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .removeSuffix("/api") + "/ws/chat"
}

