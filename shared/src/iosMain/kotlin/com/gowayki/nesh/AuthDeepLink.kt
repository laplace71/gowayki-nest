package com.gowayki.nesh

import com.gowayki.nesh.app.di.AppDi

/**
 * Puente Swift → Kotlin para deep links OAuth (`com.gowayki.nesh://login-callback`).
 * Fase actual (JSON/fake): no-op. Cuando haya backend, redirigir aquí al
 * manejador de deep links real.
 */
fun handleAuthDeepLink(urlString: String) {
    if (!AppDi.started) AppDi.start()
}