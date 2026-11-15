package com.gowayki.nesh.app.common.ui.shared

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext

/**
 * Actual Android — detección precisa de dispositivos de gama de entrada o media modesta
 * (SoCs Helio/Mali-G5x, RAM <= 4.5 GB, núcleos limitados) para calibrar animaciones y
 * sombras pesadas en Compose, manteniendo una tasa de cuadros estable de 60-120 FPS.
 */
actual object WaykiPerf {

    private var cachedIsLowEnd: Boolean? = null

    actual val isLowEnd: Boolean
        get() {
            return cachedIsLowEnd ?: detectLowEnd().also { cachedIsLowEnd = it }
        }

    actual val heavyAnimationsEnabled: Boolean get() = !isLowEnd

    actual val softShadowsEnabled: Boolean get() = !isLowEnd

    private fun detectLowEnd(): Boolean {
        // 1. Hardware y SoCs conocidos por sufrir jank en Compose con sombras/shimmer
        val hardware = (Build.HARDWARE ?: "").lowercase()
        val board = (Build.BOARD ?: "").lowercase()
        val model = (Build.MODEL ?: "").lowercase()

        val weakSoc = listOf(
            "helio", "mt67", "mt68", "mali-g5", "mali-g3", "mali-4",
            "adreno 610", "adreno 612", "unisoc", "spreadtrum", "sc98", "ums"
        )
        if (weakSoc.any { it in hardware || it in board || it in model }) return true

        // 2. RAM real disponible en el sistema (ActivityManager)
        val context = AndroidSosContext.appContext
        val (lowRam, ramGb) = runCatching {
            val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return@runCatching (false to null)
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val totalGb = mi.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            (am.isLowRamDevice || totalGb <= 4.5) to totalGb
        }.getOrDefault(false to null)

        if (lowRam) return true

        // 3. Procesadores modestos
        val fewCores = Runtime.getRuntime().availableProcessors() <= 6
        return fewCores && (ramGb == null || ramGb <= 6.0)
    }
}
