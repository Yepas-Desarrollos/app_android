package mx.checklist.data.api.dto

// DTO para operaciones bulk de templates
data class BulkStatusUpdateDto(
    val ids: List<Long>,
    val isActive: Boolean
)

// Respuesta de operaciones bulk
data class BulkStatusUpdateRes(
    val updatedCount: Int
)