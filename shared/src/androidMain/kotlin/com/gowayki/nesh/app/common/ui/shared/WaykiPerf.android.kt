package com.gowayki.nesh.app.common.ui.shared

import android.os.Build

/**
 * Actual Android — detecta gama baja por fabricante/modelo/hardware.
 */
actual object WaykiPerf {

    actual val isLowEnd: Boolean = run {
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
        val model = (Build.MODEL ?: "").lowercase()
        val hardware = (Build.HARDWARE ?: "").lowercase()

        val weakMakers = listOf("xiaomi", "redmi", "oppo", "vivo", "realme")
        val weakModels = listOf(
            "redmi", "poco", "galaxy a", "galaxy m", "note 14", "note 13", "note 12",
            "helio", "mediatek", "mali-g57", "mali-g52", "mali-g51",
        )

        weakMakers.any { it in manufacturer || it in model } ||
            weakModels.any { it in model } ||
            weakModels.any { it in hardware }
    }

    actual val heavyAnimationsEnabled: Boolean get() = !isLowEnd

    actual val softShadowsEnabled: Boolean get() = !isLowEnd
}
