package mx.checklist.data.api.dto

data class TemplateDto(
    val id: Long,
    val name: String,
    val version: Int? = null,
    val scope: String? = null, // "Auditores" | "Supervisores" | null
    val frequency: String? = null, // puede venir null
    val isActive: Boolean,
    val createdBy: Long? = null,
    val createdAt: String? = null
)
