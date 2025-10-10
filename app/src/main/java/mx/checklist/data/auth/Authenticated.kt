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

    /**
     * Obtiene el nombre formateado para mostrar: Nombre + Primera letra del apellido
     * Ejemplo: "Rodriguez Perez Juan Carlos" → "Juan R."
     * Formato BD: Apellido(s) + Nombre(s)
     */
    fun getDisplayName(): String {
        if (fullName.isNullOrBlank()) return "Usuario"

        val parts = fullName.trim().split("\\s+".toRegex())

        // Si solo hay una palabra, retornarla
        if (parts.size == 1) return parts[0]

        // Estrategia mejorada:
        // Los apellidos típicamente son las primeras 2 palabras
        // Los nombres son las siguientes palabras
        // Ejemplo: "DEL TORO BARRAGAN AMADO EMILIO"
        // Apellidos: "DEL TORO" (o podría ser "DEL TORO BARRAGAN")
        // Nombres: "AMADO EMILIO"

        // Buscar el primer nombre (asumiendo máximo 2-3 palabras para apellidos)
        val firstName = when {
            parts.size >= 4 -> parts[3] // Si hay 4+ palabras, la 4ta es probablemente el primer nombre
            parts.size == 3 -> parts[2] // Si hay 3, la 3ra es el nombre
            else -> parts.last() // Fallback: última palabra
        }

        val lastNameInitial = parts.first().first().uppercaseChar()

        return "$firstName $lastNameInitial."
    }
}