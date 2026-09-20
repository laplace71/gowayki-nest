package com.gowayki.nesh.core.config

/**
 * Configuración generada automáticamente desde .env por Gradle.
 * NO editar directamente este archivo. Edita el .env en la raíz del proyecto.
 */
object AppConfig {
    const val APP_TITLE: String = "Gowayki Nesh"
    const val SHOW_DEBUG_BANNER: Boolean = false

    // Supabase (inyectado desde .env)
    const val SUPABASE_URL: String = "https://bvxgdlawivdoiornfigo.supabase.co"
    const val SUPABASE_ANON_KEY: String = "sb_publishable_-_Vc3gCY-q48pGHTjmFaog_zYFLbf8c"

    // Google OAuth Web Client ID (ComposeAuth / Credential Manager)
    const val GOOGLE_WEB_CLIENT_ID: String = "21304726166-16dakb4a1qfjl6d4qpsifnlr3m1q4sl3.apps.googleusercontent.com"

    // Deep link OAuth (Auth scheme/host)
    const val AUTH_SCHEME: String = "com.gowayki.nesh"
    const val AUTH_HOST: String = "login-callback"
}