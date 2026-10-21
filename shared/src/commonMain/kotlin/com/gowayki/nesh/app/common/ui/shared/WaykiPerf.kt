package com.gowayki.nesh.app.common.ui.shared

/**
 * Detector de dispositivos de gama baja para animaciones pesadas.
 * En commonMain es `expect`; cada plataforma provee el `actual`.
 */
expect object WaykiPerf {

    /** true si el dispositivo tiene GPU débil (o es emulador). */
    val isLowEnd: Boolean

    /** Animaciones pesadas (canvas con 6 floats) solo en gama alta. */
    val heavyAnimationsEnabled: Boolean

    /** Sombras suaves sin blur grande en low-end. */
    val softShadowsEnabled: Boolean
}
