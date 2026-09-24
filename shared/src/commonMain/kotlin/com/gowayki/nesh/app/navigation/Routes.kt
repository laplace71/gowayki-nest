package com.gowayki.nesh.app.navigation

// Rutas con tipo (evita typos de strings sueltos que crashean en runtime).
// Tu flujo: estas rutas SON el sign-in (tu LoginScreen reemplaza al sign_in del KMP).
object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PIN = "pin"
}