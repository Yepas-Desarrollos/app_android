package mx.checklist.data.api

import mx.checklist.data.api.dto.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface Api {
    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): TokenRes

    @GET("stores")
    suspend fun stores(): List<StoreDto>

    @GET("templates")
    suspend fun templates(): List<TemplateDto>

    // Templates paginados
    @GET("templates")
    suspend fun templatesPaginated(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PaginatedTemplatesResponse

    @POST("runs")
    suspend fun createRun(@Body body: CreateRunReq): RunRes

    @GET("runs/{id}/items")
    suspend fun runItems(@Path("id") runId: Long): List<RunItemDto>

    @POST("items/{id}/respond")
    suspend fun respond(@Path("id") itemId: Long, @Body body: RespondReq): RunItemDto

    @PATCH("runs/{id}/submit")
    suspend fun submit(@Path("id") runId: Long): RunRes

    // Info del run (status, templateName, storeCode)
    @GET("runs/info/{runId}")
    suspend fun runInfo(@Path("runId") runId: Long): RunInfoDto

    // Borradores
    @GET("runs/pending")
    suspend fun pendingRuns(
        @Query("limit") limit: Int? = null,
        @Query("all") all: Boolean? = null,
        @Query("storeCode") storeCode: String? = null
    ): List<RunSummaryDto>

    // Borradores paginados
    @GET("runs/pending")
    suspend fun pendingRunsPaginated(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PaginatedRunsResponse

    // Historial (enviadas)
    @GET("runs/history")
    suspend fun historyRuns(
        @Query("limit") limit: Int? = null,
        @Query("storeCode") storeCode: String? = null
    ): List<RunSummaryDto>

    // Historial paginado
    @GET("runs/history")
    suspend fun historyRunsPaginated(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PaginatedRunsResponse

    // Eliminar borrador
    @DELETE("runs/{id}")
    suspend fun deleteRun(@Path("id") runId: Long): Unit

    // === Evidencias ===
    @Multipart
    @POST("items/{id}/attachments")
    suspend fun uploadAttachments(
        @Path("id") itemId: Long,
        @Part files: List<MultipartBody.Part>
    ): AttachmentsUploadResultDto // Changed to use the typealias

    @POST("items/{id}/attachments/list")
    suspend fun listAttachments(@Path("id") itemId: Long): List<AttachmentDto>

    @POST("items/{itemId}/attachments/{attachmentId}/delete") // Adjusted path parameters
    suspend fun deleteAttachment(
        @Path("itemId") itemId: Long,
        @Path("attachmentId") attachmentId: Int
    ): DeleteAttachmentRes

    // === ADMIN ENDPOINTS ===
    
    // Templates CRUD
    @GET("admin/templates")
    suspend fun adminGetTemplates(): List<AdminTemplateDto>

    // Admin templates paginados
    @GET("admin/templates")
    suspend fun adminGetTemplatesPaginated(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PaginatedAdminTemplatesResponse

    @POST("admin/templates")
    suspend fun adminCreateTemplate(@Body body: CreateTemplateDto): CreateTemplateRes

    @GET("admin/templates/{id}")
    suspend fun adminGetTemplate(@Path("id") templateId: Long): AdminTemplateDto

    @PATCH("admin/templates/{id}")
    suspend fun adminUpdateTemplate(
        @Path("id") templateId: Long,
        @Body body: UpdateTemplateDto
    ): AdminTemplateDto

    @DELETE("admin/templates/{id}")
    suspend fun adminDeleteTemplate(@Path("id") templateId: Long): DeleteRes

    // Items CRUD
    @POST("admin/templates/{templateId}/items")
    suspend fun adminCreateItem(
        @Path("templateId") templateId: Long,
        @Body body: CreateItemTemplateDto
    ): CreateItemTemplateRes

    @PATCH("admin/templates/{templateId}/items/{id}")
    suspend fun adminUpdateItem(
        @Path("templateId") templateId: Long,
        @Path("id") itemId: Long,
        @Body body: UpdateItemTemplateDto
    ): ItemTemplateDto

    @DELETE("admin/templates/{templateId}/items/{id}")
    suspend fun adminDeleteItem(
        @Path("templateId") templateId: Long,
        @Path("id") itemId: Long
    ): DeleteRes

    // Template status management
    @PATCH("admin/templates/{id}/status")
    suspend fun adminUpdateTemplateStatus(
        @Path("id") templateId: Long,
        @Body body: UpdateTemplateStatusDto
    ): TemplateStatusRes

    // Force delete submitted runs
    @DELETE("admin/runs/{id}/force")
    suspend fun adminForceDeleteRun(@Path("id") runId: Long): ForceDeleteRunRes
}
