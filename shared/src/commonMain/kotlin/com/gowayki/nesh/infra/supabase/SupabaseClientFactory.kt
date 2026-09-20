package com.gowayki.nesh.infra.supabase

import com.gowayki.nesh.core.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

fun createNeshSupabaseClient(): SupabaseClient {
    require(AppConfig.SUPABASE_URL.isNotBlank() && AppConfig.SUPABASE_ANON_KEY.isNotBlank()) {
        "Supabase no está configurado. Revisa SUPABASE_URL y SUPABASE_ANON_KEY en .env"
    }

    return createSupabaseClient(
        supabaseUrl = AppConfig.SUPABASE_URL,
        supabaseKey = AppConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            scheme = AppConfig.AUTH_SCHEME
            host = AppConfig.AUTH_HOST
        }
        install(ComposeAuth) {
            googleNativeLogin(serverClientId = AppConfig.GOOGLE_WEB_CLIENT_ID)
        }
        install(Postgrest)
    }
}
