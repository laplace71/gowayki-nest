package com.gowayki.nesh.core.theme.src.core

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Radios de borde (border radius) de la app.
 *
 * Escala
 * [none]  ->   0 dp  (sin redondeo)
 * [xs]    ->   4 dp
 * [sm]    ->   8 dp
 * [md]    ->  12 dp  (cards estandar)
 * [lg]    ->  16 dp  (modales, sheets)
 * [xl]    ->  24 dp
 * [full]  -> 999 dp  (pill / circulo)
 *
 * Uso
 * ```kotlin
 * Surface(shape = AppRadius.cardShape)
 * Box(modifier = Modifier.clip(AppRadius.md))
 * ```
 */
object AppRadius {

    // --- Escala base ---

    val none  = RoundedCornerShape(0.dp)
    val xs    = RoundedCornerShape(4.dp)
    val sm    = RoundedCornerShape(8.dp)
    val md    = RoundedCornerShape(12.dp)
    val lg    = RoundedCornerShape(16.dp)
    val xl    = RoundedCornerShape(24.dp)
    val full  = CircleShape

    // --- Semanticos ---

    /** Radio estandar para cards. */
    val card   : Shape = md

    /** Radio estandar para botones. */
    val button : Shape = sm

    /** Radio estandar para inputs / text fields. */
    val input  : Shape = sm

    /** Radio estandar para modales y bottom sheets (solo borde superior). */
    val modal  : Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    /** Radio estandar para chips y badges. */
    val chip   : Shape = full

    /** Radio estandar para snackbars. */
    val snackbar: Shape = sm

    /** Radio estandar para tooltips. */
    val tooltip : Shape = xs

    /** Radio estandar para FABs extendidos. */
    val fabExtended: Shape = full

    // --- Wayki (flujo auth) ---

    /** Pastilla de botones/campos (50 dp). */
    val pill   : Shape = RoundedCornerShape(50.dp)

    /** Casilla de PIN (20 dp). */
    val pinBox : Shape = RoundedCornerShape(20.dp)

    /** Tarjeta hero de bienvenida (32 dp). */
    val hero   : Shape = RoundedCornerShape(32.dp)
}