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

    // Historial (enviadas)
    @GET("runs/history")
    suspend fun historyRuns(
        @Query("limit") limit: Int? = null,
        @Query("storeCode") storeCode: String? = null
    ): List<RunSummaryDto>

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
}
