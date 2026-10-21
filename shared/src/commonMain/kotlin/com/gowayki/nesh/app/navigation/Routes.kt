package com.gowayki.nesh.app.navigation

// Rutas con tipo (evita typos de strings sueltos que crashean en runtime).
// Tu flujo: estas rutas SON el sign-in (tu LoginScreen reemplaza al sign_in del KMP).
object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RECOVER_ACCESS = "recover_access"
    const val PIN = "pin"
    const val REPORTS = "reports"
    const val HOME = "home"
    const val SOS = "sos"
    const val COMMUNICATION = "communication"
    const val NEW_GROUP = "new_group"
    const val ROUTE_DETAIL = "route_detail"
    const val SETTINGS = "settings"
    const val MAP = "map"

    /**
     * Ruta del chat. Lleva placeholder porque exige un argumento:
     * al navegar se usa "chat/<conversationId>", NO "chat" a secas
     * (registrarla sin el placeholder revienta la app al arrancar).
     */
    const val CHAT_PATTERN = "chat/{conversationId}"

    /** Construye la ruta real del chat para una conversación. */
    fun chat(conversationId: String): String = "chat/$conversationId"
}