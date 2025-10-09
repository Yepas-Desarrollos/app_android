package mx.checklist.data.api.dto

data class RunSummaryDto(
    val id: Long,
    val templateId: Long,
    val storeId: Long,
    val status: String,
    val templateName: String?,
    val storeCode: String?,
    val totalCount: Int,
    val answeredCount: Int,
    val updatedAt: String, // ISO datetime del backend
    val assignedTo: AssignedToDto? = null // ✅ NUEVO: Información de quién respondió
)

/**
 * Información del usuario que respondió el checklist
 */
data class AssignedToDto(
    val id: Long,
    val name: String,
    val email: String,
    val roleCode: String? = null
)
