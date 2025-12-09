package mx.checklist.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ============================================
// RESPUESTAS DE LISTAS
// ============================================

/**
 * Item de lista de revisiones pendientes (para SUPERVISORES)
 * Endpoint: GET /audit-reviews/pending
 */
@JsonClass(generateAdapter = true)
data class PendingReviewDto(
    @Json(name = "id") val id: Long,
    @Json(name = "auditItemTitle") val auditItemTitle: String,
    @Json(name = "auditItemCategory") val auditItemCategory: String?,
    @Json(name = "storeName") val storeName: String,
    @Json(name = "storeCode") val storeCode: String,
    @Json(name = "auditNotes") val auditNotes: String?,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "createdByUserName") val createdByUserName: String,
    @Json(name = "checklistTemplateName") val checklistTemplateName: String? = null
)

/**
 * Item de lista de revisiones corregidas (para AUDITORES)
 * Endpoint: GET /audit-reviews/corrected
 */
@JsonClass(generateAdapter = true)
data class CorrectedReviewDto(
    @Json(name = "id") val id: Long,
    @Json(name = "auditItemTitle") val auditItemTitle: String,
    @Json(name = "checklistTemplateName") val checklistTemplateName: String? = null,
    @Json(name = "storeName") val storeName: String,
    @Json(name = "storeCode") val storeCode: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "correctedAt") val correctedAt: String,
    @Json(name = "correctedByUserName") val correctedByUserName: String,
    @Json(name = "correctionNotes") val correctionNotes: String?,
    @Json(name = "correctionPhotos") val correctionPhotos: List<String>
)

// ============================================
// DETALLE DE REVISIÓN
// ============================================

/**
 * Detalle completo de una revisión
 * Endpoint: GET /audit-reviews/:id
 */
@JsonClass(generateAdapter = true)
data class ReviewDetailDto(
    @Json(name = "id") val id: Long,
    @Json(name = "status") val status: String,
    @Json(name = "auditNotes") val auditNotes: String?,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "auditItem") val auditItem: AuditItemInfoDto,
    @Json(name = "store") val store: StoreInfoDto,
    @Json(name = "createdByUser") val createdByUser: UserInfoDto,
    @Json(name = "corrections") val corrections: List<CorrectionDetailDto>
)

@JsonClass(generateAdapter = true)
data class AuditItemInfoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
    @Json(name = "category") val category: String?,
    @Json(name = "responseStatus") val responseStatus: String
)

@JsonClass(generateAdapter = true)
data class StoreInfoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "code") val code: String,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class UserInfoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class CorrectionDetailDto(
    @Json(name = "id") val id: Long,
    @Json(name = "correctionNotes") val correctionNotes: String?,
    @Json(name = "correctedAt") val correctedAt: String,
    @Json(name = "correctedByUser") val correctedByUser: UserInfoDto,
    @Json(name = "attachments") val attachments: List<CorrectionAttachmentDto>
)

@JsonClass(generateAdapter = true)
data class CorrectionAttachmentDto(
    @Json(name = "id") val id: Int,
    @Json(name = "url") val url: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)

// ============================================
// CORRECCIONES DEL EQUIPO (MGR_OPS)
// ============================================

/**
 * Item de correcciones del equipo
 * Endpoint: GET /audit-corrections/my-team
 */
@JsonClass(generateAdapter = true)
data class TeamCorrectionDto(
    @Json(name = "id") val id: Long,
    @Json(name = "reviewId") val reviewId: Long,
    @Json(name = "auditItemTitle") val auditItemTitle: String,
    @Json(name = "storeName") val storeName: String,
    @Json(name = "storeCode") val storeCode: String,
    @Json(name = "correctedByUser") val correctedByUser: UserInfoDto,
    @Json(name = "correctionNotes") val correctionNotes: String?,
    @Json(name = "correctedAt") val correctedAt: String,
    @Json(name = "attachments") val attachments: List<CorrectionAttachmentDto>
)

// ============================================
// PAGINACIÓN
// ============================================

@JsonClass(generateAdapter = true)
data class ReviewPaginationDto(
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "total") val total: Int,
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "hasMore") val hasMore: Boolean
)

@JsonClass(generateAdapter = true)
data class PaginatedPendingReviewsResponse(
    @Json(name = "data") val data: List<PendingReviewDto>,
    @Json(name = "pagination") val pagination: ReviewPaginationDto
)

@JsonClass(generateAdapter = true)
data class PaginatedCorrectedReviewsResponse(
    @Json(name = "data") val data: List<CorrectedReviewDto>,
    @Json(name = "pagination") val pagination: ReviewPaginationDto
)

@JsonClass(generateAdapter = true)
data class PaginatedTeamCorrectionsResponse(
    @Json(name = "data") val data: List<TeamCorrectionDto>,
    @Json(name = "pagination") val pagination: ReviewPaginationDto
)

/**
 * Respuesta genérica paginada para los endpoints de reviews
 * Usada por Api.kt con diferentes tipos de DTOs
 */
@JsonClass(generateAdapter = true)
data class PaginatedReviewsResponse<T>(
    @Json(name = "data") val data: List<T>,
    @Json(name = "pagination") val pagination: ReviewPaginationDto
)

// ============================================
// REQUESTS
// ============================================

/**
 * Request para crear una corrección
 * Endpoint: POST /audit-corrections
 */
@JsonClass(generateAdapter = true)
data class CreateCorrectionRequest(
    @Json(name = "reviewId") val reviewId: Long,
    @Json(name = "correctionNotes") val correctionNotes: String?
)

/**
 * Response de crear una corrección
 */
@JsonClass(generateAdapter = true)
data class CreateCorrectionResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "reviewId") val reviewId: Long,
    @Json(name = "correctedAt") val correctedAt: String
)

/**
 * Request para validar una corrección
 * Endpoint: PATCH /audit-reviews/:id/validate
 */
@JsonClass(generateAdapter = true)
data class ValidateCorrectionRequest(
    @Json(name = "approved") val approved: Boolean
)

/**
 * Response de validar una corrección
 */
@JsonClass(generateAdapter = true)
data class ValidateCorrectionResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "status") val status: String,
    @Json(name = "updatedAt") val updatedAt: String
)

// ============================================
// MIS CORRECCIONES (SUPERVISOR)
// ============================================

/**
 * Item de lista de correcciones del supervisor
 * Endpoint: GET /audit-corrections/mine
 */
@JsonClass(generateAdapter = true)
data class MyCorrectionDto(
    @Json(name = "id") val id: Long,
    @Json(name = "reviewId") val reviewId: Long,
    @Json(name = "auditItemTitle") val auditItemTitle: String,
    @Json(name = "checklistTemplateName") val checklistTemplateName: String?,
    @Json(name = "storeName") val storeName: String,
    @Json(name = "storeCode") val storeCode: String,
    @Json(name = "status") val status: String, // CORRECTED=En revisión, VALIDATED=Aprobado, REJECTED=Rechazado
    @Json(name = "correctionNotes") val correctionNotes: String?,
    @Json(name = "correctedAt") val correctedAt: String,
    @Json(name = "attachmentsCount") val attachmentsCount: Int,
    @Json(name = "attachments") val attachments: List<CorrectionAttachmentDto>
)
