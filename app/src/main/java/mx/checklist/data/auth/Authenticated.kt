package mx.checklist.data.auth

/**
 * Datos de usuario autenticado
 */
data class Authenticated(
    val token: String,
    val roleCode: String,
    val userId: String? = null,
    val email: String? = null,
    val fullName: String? = null
) {
    /**
     * Verifica si el usuario tiene un rol específico
     */
    fun hasRole(role: String): Boolean {
        return roleCode.equals(role, ignoreCase = true)
    }
    
    /**
     * Verifica si el usuario es administrador (cualquier tipo)
     */
    fun isAdmin(): Boolean {
        return hasRole("ADMIN")
    }
    
    /**
     * Verifica si el usuario es manager (cualquier área)
     */
    fun isManager(): Boolean {
        return hasRole("MGR_PREV") || hasRole("MGR_OPS") || isAdmin()
    }
    
    /**
     * Verifica si el usuario es manager de prevención
     */
    fun isPreventionManager(): Boolean {
        return hasRole("MGR_PREV") || isAdmin()
    }
    
    /**
     * Verifica si el usuario es manager de operaciones
     */
    fun isOperationsManager(): Boolean {
        return hasRole("MGR_OPS") || isAdmin()
    }
    
    /**
     * Verifica si el usuario tiene permisos de administración (ADMIN o MANAGER)
     */
    fun hasAdminPermissions(): Boolean {
        return isManager()
    }
    
    /**
     * Verifica si puede borrar corridas enviadas (solo ADMIN)
     */
    fun canDeleteSubmittedRuns(): Boolean {
        return isAdmin()
    }
    
    /**
     * Verifica si puede ver corridas de otros usuarios
     */
    fun canViewOthersRuns(): Boolean {
        return isManager() // Managers y admin pueden ver corridas de otros
    }
    
    /**
     * Verifica si debe filtrar corridas por usuario propio (AUDITOR/SUPERVISOR)
     */
    fun shouldFilterOwnRuns(): Boolean {
        return hasRole("AUDITOR") || hasRole("SUPERVISOR")
    }
    
    /**
     * Obtiene el nombre del rol en formato legible
     */
    fun getRoleName(): String {
        return when (roleCode.uppercase()) {
            "ADMIN" -> "Administrador"
            "MGR_PREV" -> "Manager Prevención"
            "MGR_OPS" -> "Manager Operaciones"
            "AUDITOR" -> "Auditor"
            "SUPERVISOR" -> "Supervisor"
            else -> "Usuario"
        }
    }
}