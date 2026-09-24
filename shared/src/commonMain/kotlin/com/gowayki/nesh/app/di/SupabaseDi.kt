package com.gowayki.nesh.app.di

import io.github.jan.supabase.SupabaseClient

/**
 * Módulo DI de Supabase (DORMIDO en fase JSON).
 *
 * La app corre con fakes/JSON local y no necesita [SupabaseClient].
 * Para reactivar la base de datos:
 *  1. Restaurar dependencias supabase/ktor/serialization en libs.versions.toml
 *     y shared/build.gradle.kts.
 *  2. Descomentar `infra/supabase/SupabaseClientFactory.kt`.
 *  3. En [install]: `AppDi.registerLazy<SupabaseClient> { createNeshSupabaseClient() }`
 *  4. Cambiar AuthDi para registrar `SupabaseAuthRepository`.
 */
object SupabaseDi {
    fun install() {
        // Sin cliente real en fase JSON: el fake no lo necesita.
    }

    /** Falla a propósito mientras Supabase esté dormido. */
    fun client(): SupabaseClient =
        error("Supabase desactivado (fase JSON). Reactívalo en infra/supabase/SupabaseClientFactory.kt")

    /** Útil en tests: sustituye el cliente. */
    fun register(client: SupabaseClient) {
        AppDi.register(client)
    }
}