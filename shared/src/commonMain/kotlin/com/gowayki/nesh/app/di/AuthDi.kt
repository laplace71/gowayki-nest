package com.gowayki.nesh.app.di

/**
 * Módulo DI de autenticación (DORMIDO en fase JSON).
 *
 * El flujo actual no pasa por AppDi: AuthViewModel construye directamente
 * FakeAuthRepository (data/repository).
 * Para reactivar la base de datos:
 *  1. Descomentar `infra/auth/SupabaseAuthRepository.kt` y
 *     `infra/supabase/SupabaseClientFactory.kt`.
 *  2. Aquí: `AppDi.registerLazy<AuthRepository> { SupabaseAuthRepository(SupabaseDi.client()) }`
 *  3. Registrar también los usecases que consuman AppDi.
 */
object AuthDi {
    fun install() {
        // no-op en fase JSON: la app usa el fake directo.
    }
}