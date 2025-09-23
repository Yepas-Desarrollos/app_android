package mx.checklist.config

/**
 * Configuración de características de la aplicación
 */
object AppConfig {
    
    /**
     * Habilitar optimizaciones de paginación
     * 
     * true = Usa las pantallas optimizadas con paginación automática
     * false = Usa las pantallas originales (sin paginación)
     * 
     * Para resolver el problema de "se atora al bajar", cambiar a true
     */
    const val ENABLE_PAGINATION_OPTIMIZATIONS = true
    
    /**
     * Configuración de paginación
     */
    object Pagination {
        const val DEFAULT_PAGE_SIZE = 20
        const val LOAD_MORE_THRESHOLD = 3  // Cargar más cuando queden 3 elementos
        const val MAX_PAGE_SIZE = 50
    }
    
    /**
     * Configuración de performance
     */
    object Performance {
        const val ENABLE_TEMPLATE_CACHING = true
        const val CACHE_EXPIRY_MINUTES = 5
        const val ENABLE_RETRY_STRATEGY = true
        const val MAX_RETRY_ATTEMPTS = 3
    }
    
    /**
     * Configuración de debug
     */
    object Debug {
        const val ENABLE_PAGINATION_LOGS = true
        const val SHOW_PAGINATION_INFO = true  // Mostrar info de paginación en UI
        const val ENABLE_PERFORMANCE_METRICS = false
    }
}