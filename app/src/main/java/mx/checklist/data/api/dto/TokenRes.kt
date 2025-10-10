package mx.checklist.data.api.dto

data class TokenRes(
    val access_token: String,
    val roleCode: String? = null, // ADMIN | AUDITOR | SUPERVISOR
    val userId: String? = null,
    val email: String? = null,
    val fullName: String? = null // Formato: "Apellido(s) Nombre(s)"
)
