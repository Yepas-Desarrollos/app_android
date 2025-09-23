package mx.checklist.data.api.dto

// Response paginado para templates
data class PaginatedTemplatesResponse(
    val data: List<TemplateDto>,
    val pagination: PaginationDto
)

// Response paginado para runs
data class PaginatedRunsResponse(
    val data: List<RunSummaryDto>,
    val pagination: PaginationDto
)

// Response paginado para admin templates
data class PaginatedAdminTemplatesResponse(
    val data: List<AdminTemplateDto>,
    val pagination: PaginationDto
)

// Información de paginación del servidor
data class PaginationDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

// Para manejar el estado de paginación en el cliente
data class PaginationInfo(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 0,
    val hasMore: Boolean = false
)