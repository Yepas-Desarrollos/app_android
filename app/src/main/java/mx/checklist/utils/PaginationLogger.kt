package mx.checklist.utils

import android.util.Log
import mx.checklist.config.AppConfig

/**
 * Utilidades para monitoreo de performance de paginación
 */
object PaginationLogger {
    
    private const val TAG = "PaginationOpt"
    
    fun logPageLoad(
        screen: String,
        page: Int,
        itemsLoaded: Int,
        totalItems: Int,
        loadTimeMs: Long
    ) {
        if (AppConfig.Debug.ENABLE_PAGINATION_LOGS) {
            Log.d(TAG, """
                📄 Página cargada:
                - Pantalla: $screen
                - Página: $page
                - Items cargados: $itemsLoaded
                - Total items: $totalItems
                - Tiempo: ${loadTimeMs}ms
            """.trimIndent())
        }
    }
    
    fun logScrollTrigger(
        screen: String,
        position: Int,
        threshold: Int,
        hasMore: Boolean
    ) {
        if (AppConfig.Debug.ENABLE_PAGINATION_LOGS) {
            Log.d(TAG, """
                🔽 Trigger de scroll:
                - Pantalla: $screen
                - Posición: $position
                - Threshold: $threshold
                - Hay más páginas: $hasMore
            """.trimIndent())
        }
    }
    
    fun logLoadError(screen: String, error: String) {
        Log.e(TAG, "❌ Error en $screen: $error")
    }
    
    fun logOptimizationEnabled(screen: String, enabled: Boolean) {
        Log.i(TAG, """
            ⚡ Optimización de paginación ${if (enabled) "HABILITADA" else "DESHABILITADA"} para $screen
        """.trimIndent())
    }
}