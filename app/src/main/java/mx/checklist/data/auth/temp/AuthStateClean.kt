package mx.checklist.data.auth

/**
 * Estado global de autenticación limpio
 * Mantiene el token y roleCode del usuario actual
 */
object AuthStateClean {
    @Volatile
    var token: String? = null
    
    @Volatile
    var roleCode: String? = null
    
    /**
     * Objeto Authenticated actual (si está autenticado)
     */
    val authenticated: Authenticated?
        get() {
            return if (token != null && roleCode != null) {
                Authenticated(token = token!!, roleCode = roleCode!!)
            } else {
                null
            }
        }
    
    /**
     * Limpia todo el estado de autenticación
     */
    fun clear() {
        token = null
        roleCode = null
    }
    
    /**
     * Verifica si el usuario está autenticado
     */
    fun isAuthenticated(): Boolean {
        return token != null && roleCode != null
    }
}