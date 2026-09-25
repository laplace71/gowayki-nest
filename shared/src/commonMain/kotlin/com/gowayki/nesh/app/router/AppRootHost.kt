package com.gowayki.nesh.app.router

import androidx.compose.runtime.Composable
import com.gowayki.nesh.app.navigation.WaykiNav

// Misma ruta que el AppRootHost del git, con tu diseño: delega a tu NavHost
// (tu LoginScreen ES el sign-in; welcome → login → register → pin).
@Composable
fun AppRootHost() {
    WaykiNav()
}