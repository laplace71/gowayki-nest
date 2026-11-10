package com.gowayki.nesh.core.config

/**
 * Interruptores de la fase beta.
 *
 * Mientras [selfAssignRoutes] esté en true, el chofer puede elegir su línea y
 * unidad desde la ventana emergente. Cuando entre el panel de administración,
 * se pone en false y la asignación queda solo para la empresa.
 */
object BetaFlags {
    const val selfAssignRoutes: Boolean = true
}
