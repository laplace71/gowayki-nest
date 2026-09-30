package com.gowayki.nesh.core.theme.src.core

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado (spacing scale) de la app.
 *
 * Escala
 * [none]  ->   0 dp
 * [xxs]   ->   2 dp
 * [xs]    ->   4 dp
 * [sm]    ->   8 dp
 * [md]    ->  12 dp
 * [lg]    ->  16 dp
 * [xl]    ->  24 dp
 * [xxl]   ->  32 dp
 * [xxxl]  ->  48 dp
 * [huge]  ->  64 dp
 *
 * Uso
 * ```kotlin
 * Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
 * Spacer(modifier = Modifier.height(AppSpacing.xl))
 * Arrangement.spacedBy(AppSpacing.md)
 * ```
 */
object AppSpacing {

    val none : Dp =  0.dp
    val xxs  : Dp =  2.dp
    val xs   : Dp =  4.dp
    val sm   : Dp =  8.dp
    val md   : Dp = 12.dp
    val lg   : Dp = 16.dp
    val xl   : Dp = 24.dp
    val xxl  : Dp = 32.dp
    val xxxl : Dp = 48.dp
    val huge : Dp = 64.dp

    // --- Semanticos ---

    /** Padding interno de cards. */
    val cardPadding     : Dp = lg

    /** Padding horizontal global de la pantalla. */
    val screenPadding   : Dp = lg

    /** Separacion entre items de una lista. */
    val listItemSpacing : Dp = sm

    /** Separacion entre items de una fila de chips. */
    val chipSpacing     : Dp = xs

    /** Separacion entre icono y texto en botones/items. */
    val iconText        : Dp = sm

    /** Padding interno de inputs y text fields. */
    val inputPadding    : Dp = md

    /** Margen inferior del FAB (above bottom nav). */
    val fabBottomMargin : Dp = xxl

    /** Alto minimo de un item de lista tactil. */
    val minTouchTarget  : Dp = 48.dp

    // --- Iconos (absorbidos de AppIconSizes: UNICO lugar de tamanos) ---

    val iconXs   : Dp = 12.dp
    val iconSm   : Dp = 16.dp
    val iconMd   : Dp = 24.dp
    val iconLg   : Dp = 32.dp
    val iconXl   : Dp = 48.dp
    val iconXxl  : Dp = 64.dp
    val iconHuge : Dp = 96.dp

    val iconTextField  : Dp = iconMd
    val iconListItem   : Dp = iconMd
    val iconNavBar     : Dp = iconMd
    val iconFab        : Dp = iconMd
    val iconIconButton : Dp = iconMd
    val iconAvatarSm   : Dp = iconXl
    val iconAvatarLg   : Dp = iconXxl

    // --- Wayki (flujo auth): UNICO lugar de tamanos del flujo ---

    /** Padding horizontal de pantalla (welcome/login/registro/pin). */
    val pageHorizontal   : Dp = 20.dp

    /** Separacion tras el encabezado (debajo de la flecha atras). */
    val topSection       : Dp = 28.dp

    /** Separacion entre campos de formulario. */
    val formFieldGap     : Dp = 16.dp

    /** Separacion de seccion (p.ej. PIN -> boton). */
    val sectionGap       : Dp = 20.dp

    /** Alto de la tarjeta hero de bienvenida. */
    val heroCardHeight   : Dp = 310.dp

    /** Alto del boton primario a ancho completo. */
    val buttonPrimaryHeight : Dp = 72.dp

    /** Alto del boton primario compacto (con flecha). */
    val buttonSecondaryHeight : Dp = 56.dp

    /** Ancho fijo del boton compacto. */
    val buttonFixedWidth  : Dp = 210.dp

    /** Diametro del circulo de Google del registro. */
    val googleCircleSize  : Dp = 52.dp

    /** Icono de flecha atras (contenedor tactil). */
    val backButtonSize    : Dp = 48.dp

    /** Lienzo interno de la flecha atras. */
    val backArrowSize     : Dp = 22.dp

    /** Flecha del boton primario. */
    val arrowIconSize     : Dp = 32.dp

    /** Casilla de PIN por defecto. */
    val pinBoxDefault     : Dp = 62.dp

    /** Casilla de PIN en pantallas (login y creacion). */
    val pinBoxScreen      : Dp = 44.dp

    /** Offset vertical del bloque central en login/registro. */
    val authContentTop    : Dp = 200.dp

    /** Offset vertical del bloque central en PIN. */
    val pinContentTop     : Dp = 230.dp

    /** Margen inferior del contenido central (antes del anclado de abajo). */
    val contentBottomMargin : Dp = 80.dp

    /** Reserva inferior del contenido en bienvenida (botones anclados). */
    val welcomeBottomReserve : Dp = 190.dp

    /** Tamano del icono de bus (logo) en la tarjeta hero. */
    val busIconWidth  : Dp = 150.dp
    val busIconHeight : Dp = 160.dp

    /** Grosor de bordes finos (Google, PIN, campos). */
    val borderThin    : Dp = 2.dp
}