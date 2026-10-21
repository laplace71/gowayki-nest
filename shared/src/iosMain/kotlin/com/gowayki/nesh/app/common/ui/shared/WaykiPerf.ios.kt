package com.gowayki.nesh.app.common.ui.shared

/**
 * Actual iOS — por defecto gama alta (iPhone modernos).
 */
actual object WaykiPerf {
    actual val isLowEnd: Boolean = false
    actual val heavyAnimationsEnabled: Boolean get() = !isLowEnd
    actual val softShadowsEnabled: Boolean get() = !isLowEnd
}
