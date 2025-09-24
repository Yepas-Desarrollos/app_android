package mx.checklist.data.api.dto

/**
 * Usuario que puede ser asignado a sectores
 */
data class AssignableUserDto(
    val id: Long,
    val email: String,
    val name: String,
    val roleCode: String,
    val stores: List<AssignedStoreDto> = emptyList()
)

/**
 * Tienda asignada a un usuario
 */
data class AssignedStoreDto(
    val id: Long,
    val code: String,
    val name: String,
    val sectors: List<String> = emptyList()
)

/**
 * Resumen de asignaciones por área
 */
data class AssignmentSummaryDto(
    val storeCode: String,
    val storeName: String,
    val sector: String,
    val assignedUsers: Int,
    val users: List<UserAssignmentDto>
)

/**
 * Usuario en resumen de asignaciones
 */
data class UserAssignmentDto(
    val id: Long,
    val name: String,
    val roleCode: String
)

/**
 * Respuesta del backend para resumen de asignaciones
 */
data class AssignmentSummaryResponse(
    val success: Boolean,
    val data: List<AssignmentSummaryDto>,
    val meta: AssignmentSummaryMeta? = null
)

data class AssignmentSummaryMeta(
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val filters: AssignmentSummaryFilters? = null
)

data class AssignmentSummaryFilters(
    val userId: Long? = null,
    val sector: Int? = null
)

/**
 * Request para asignar usuario a sectores
 */
data class AssignUserToSectorsRequest(
    val userId: String,
    val sectors: List<Int>
)

/**
 * Response de asignación exitosa
 */
data class AssignmentResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)