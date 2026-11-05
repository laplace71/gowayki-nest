package com.gowayki.nesh.app.router

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gowayki.nesh.app.navigation.WaykiNav
import com.gowayki.nesh.core.theme.src.core.AppColors

// Misma ruta que el AppRootHost del git, con tu diseño: delega a tu NavHost
// (tu LoginScreen ES el sign-in; welcome → login → register → pin).
@Composable
fun AppRootHost() {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.appBackground)
    ) {
        WaykiNav()
    }
}
