package com.gowayki.nesh.app.di

import com.gowayki.nesh.infra.supabase.createNeshSupabaseClient
import io.github.jan.supabase.SupabaseClient

/**
 * Módulo DI de Supabase.
 *
 * Expone [SupabaseClient] listo para inyectar:
 * ```
 * val client = SupabaseDi.client()
 * // o
 * val client = AppDi.get<SupabaseClient>()
 * ```
 */
object SupabaseDi {
    fun install() {
        AppDi.registerLazy<SupabaseClient> { createNeshSupabaseClient() }
    }

    fun client(): SupabaseClient = AppDi.get()

    /** Útil en tests: sustituye el cliente. */
    fun register(client: SupabaseClient) {
        AppDi.register(client)
    }
}
