package mx.checklist.data.repository

import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para manejar operaciones de Revisiones y Correcciones de Auditorías
 */
@Singleton
class AuditReviewRepository @Inject constructor(
    private val api: Api
) {
    // ============================================
    // SUPERVISOR: Revisiones Pendientes
    // ============================================

    /**
     * Obtener revisiones pendientes de corrección (primera página)
     */
    suspend fun getPendingReviews(
        limit: Int = 20,
        storeId: Long? = null
    ): List<PendingReviewDto> {
        val response = api.getPendingReviews(limit = limit, page = 1, storeId = storeId)
        return response.data
    }

    /**
     * Obtener revisiones pendientes paginadas
     */
    suspend fun getPendingReviewsPaginated(
        page: Int = 1,
        limit: Int = 20,
        storeId: Long? = null
    ): PaginatedReviewsResponse<PendingReviewDto> {
        return api.getPendingReviews(limit = limit, page = page, storeId = storeId)
    }

    // ============================================
    // AUDITOR: Revisiones Corregidas
    // ============================================

    /**
     * Obtener revisiones corregidas para validar (primera página)
     */
    suspend fun getCorrectedReviews(
        limit: Int = 20,
        status: String? = null
    ): List<CorrectedReviewDto> {
        val response = api.getCorrectedReviews(limit = limit, page = 1, status = status)
        return response.data
    }

    /**
     * Obtener revisiones corregidas paginadas
     */
    suspend fun getCorrectedReviewsPaginated(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null
    ): PaginatedReviewsResponse<CorrectedReviewDto> {
        return api.getCorrectedReviews(limit = limit, page = page, status = status)
    }

    // ============================================
    // DETALLE Y VALIDACIÓN
    // ============================================

    /**
     * Obtener detalle completo de una revisión
     */
    suspend fun getReviewDetail(reviewId: Long): ReviewDetailDto {
        return api.getReviewDetail(reviewId)
    }

    /**
     * Validar una corrección (aprobar o rechazar)
     */
    suspend fun validateCorrection(
        reviewId: Long,
        approved: Boolean
    ): ValidateCorrectionResponse {
        return api.validateCorrection(
            reviewId = reviewId,
            request = ValidateCorrectionRequest(approved = approved)
        )
    }

    // ============================================
    // CREAR CORRECCIÓN
    // ============================================

    /**
     * Crear una corrección (sin fotos)
     */
    suspend fun createCorrection(
        reviewId: Long,
        notes: String?
    ): CreateCorrectionResponse {
        return api.createCorrection(
            request = CreateCorrectionRequest(
                reviewId = reviewId,
                correctionNotes = notes
            )
        )
    }

    /**
     * Subir fotos de evidencia a una corrección
     */
    suspend fun uploadCorrectionAttachments(
        correctionId: Long,
        files: List<File>
    ): List<CorrectionAttachmentDto> {
        val parts = files.map { file ->
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)
            MultipartBody.Part.createFormData("files", file.name, requestBody)
        }
        return ApiClient.uploadApi.uploadCorrectionAttachments(correctionId, parts)
    }

    /**
     * Flujo completo: Crear corrección y subir fotos
     * Este es el método principal que debe usar el ViewModel
     */
    suspend fun createCorrectionWithPhotos(
        reviewId: Long,
        notes: String?,
        photos: List<File>
    ): Pair<CreateCorrectionResponse, List<CorrectionAttachmentDto>> {
        // Paso 1: Crear la corrección
        val correctionResponse = createCorrection(reviewId, notes)
        
        // Paso 2: Subir las fotos con el ID de la corrección
        val attachmentsResponse = uploadCorrectionAttachments(
            correctionId = correctionResponse.id,
            files = photos
        )
        
        return Pair(correctionResponse, attachmentsResponse)
    }

    // ============================================
    // MGR_OPS: Correcciones del Equipo
    // ============================================

    /**
     * Obtener correcciones del equipo (primera página)
     */
    suspend fun getTeamCorrections(
        limit: Int = 20,
        storeId: Long? = null,
        userId: Long? = null
    ): List<TeamCorrectionDto> {
        val response = api.getTeamCorrections(
            limit = limit,
            page = 1,
            storeId = storeId,
            userId = userId
        )
        return response.data
    }

    /**
     * Obtener correcciones del equipo paginadas
     */
    suspend fun getTeamCorrectionsPaginated(
        page: Int = 1,
        limit: Int = 20,
        storeId: Long? = null,
        userId: Long? = null
    ): PaginatedReviewsResponse<TeamCorrectionDto> {
        return api.getTeamCorrections(
            limit = limit,
            page = page,
            storeId = storeId,
            userId = userId
        )
    }

    // ============================================
    // MIS CORRECCIONES (SUPERVISOR)
    // ============================================

    /**
     * Obtener mis correcciones (lo que he corregido como supervisor)
     */
    suspend fun getMyCorrections(
        status: String? = null
    ): List<MyCorrectionDto> {
        return api.getMyCorrections(status = status).data
    }

    /**
     * Obtener mis correcciones paginadas
     */
    suspend fun getMyCorrectionsPagenated(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null
    ): PaginatedReviewsResponse<MyCorrectionDto> {
        return api.getMyCorrections(
            limit = limit,
            page = page,
            status = status
        )
    }
}
