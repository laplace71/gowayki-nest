package com.gowayki.nesh.domain.model

/**
 * Reglas de intentos y bloqueo del login.
 *
 * Deben coincidir con el backend (`LoginUseCase`): 5 PIN incorrectos bloquean
 * la cuenta 15 minutos. Él es quien las aplica y las guarda en la base de
 * datos, así que la app solo las refleja para no mostrar un tiempo distinto.
 */
object AuthPolicy {
    const val MAX_INTENTOS: Int = 5
    const val MINUTOS_DE_BLOQUEO: Int = 15
    const val SEGUNDOS_DE_BLOQUEO: Int = MINUTOS_DE_BLOQUEO * 60
}
