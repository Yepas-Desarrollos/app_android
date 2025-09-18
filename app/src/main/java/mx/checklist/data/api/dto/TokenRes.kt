package mx.checklist.data.api.dto

data class TokenRes(
    val access_token: String,
    val roleCode: String? = null // ADMIN | AUDITOR | SUPERVISOR
)
