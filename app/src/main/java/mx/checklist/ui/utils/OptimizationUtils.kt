package mx.checklist.ui.utils

import mx.checklist.config.AppConfig

/**
 * Utilidad para determinar qué pantallas usar según configuración
 */
object OptimizationUtils {
    
    const val USE_OPTIMIZED_SCREENS = AppConfig.ENABLE_PAGINATION_OPTIMIZATIONS
    
    fun logOptimizationStatus() {
        if (USE_OPTIMIZED_SCREENS) {
            println("✅ Usando pantallas optimizadas - Problema 'se atora al bajar' resuelto")
        } else {
            println("⚠️ Usando pantallas originales - Puede haber lag en listas grandes")
        }
    }
}