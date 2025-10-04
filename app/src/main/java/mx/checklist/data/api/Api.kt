package mx.checklist.data.api

import mx.checklist.data.api.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
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

    // NUEVO: Endpoint público para obtener estructura de template (para AUDITOR/SUPERVISOR)
    // No requiere permisos de admin, solo JWT
    // Filtra automáticamente por scope del usuario
    @GET("templates/{id}/structure")
    suspend fun getTemplateStructure(@Path("id") templateId: Long): AdminTemplateDto

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

    // === ASSIGNMENT ENDPOINTS ===
    
    @GET("admin/assignments/assignable-users")
    suspend fun getAssignableUsers(): List<AssignableUserDto>
    
    @GET("admin/assignments/summary")
    suspend fun getAssignmentSummary(
        @Query("userId") userId: Long? = null,
        @Query("sector") sector: Int? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null
    ): AssignmentSummaryResponse
    
    @POST("admin/assignments/sectors")
    suspend fun assignUserToSectors(@Body body: AssignUserToSectorsRequest): AssignmentResponse
    
    @GET("admin/assignments/user/{userId}/stores")
    suspend fun getUserAssignedStores(@Path("userId") userId: String): List<AssignedStoreDto>

    @GET("admin/assignments/sectors")
    suspend fun getAssignmentSectors(): List<Int>

    // === NUEVOS ENDPOINTS: Estructura de Checklist (Secciones & Items con porcentajes) ===
    // Secciones
    @GET("admin/checklist-sections/checklist/{checklistId}")
    suspend fun getSections(@Path("checklistId") checklistId: Long): Response<List<SectionTemplateDto>>

    @POST("admin/checklist-sections/checklist/{checklistId}")
    suspend fun createSection(@Path("checklistId") checklistId: Long, @Body section: SectionTemplateCreateDto): Response<SectionTemplateDto>

    @PATCH("admin/checklist-sections/{id}")
    suspend fun updateSection(@Path("id") id: Long, @Body section: SectionTemplateUpdateDto): Response<SectionTemplateDto>

    @DELETE("admin/checklist-sections/{id}")
    suspend fun deleteSection(@Path("id") id: Long): Response<Unit>

    @PATCH("admin/checklist-sections/checklist/{checklistId}/update-percentages")
    suspend fun updateSectionPercentages(
        @Path("checklistId") checklistId: Long,
        @Body payload: SectionPercentagesPayload
    ): Response<List<SectionTemplateDto>>

    // Endpoint no disponible en backend - comentado temporalmente
    // @PATCH("admin/checklist-sections/checklist/{checklistId}/distribute-percentages")
    // suspend fun distributeSectionPercentages(@Path("checklistId") checklistId: Long): Response<List<SectionTemplateDto>>

    @PATCH("admin/checklist-sections/checklist/{checklistId}/reorder")
    suspend fun reorderSections(
        @Path("checklistId") checklistId: Long,
        @Body sectionIds: List<Long>
    ): Response<List<SectionTemplateDto>>

    // Items por sección
    @GET("admin/checklist-items/section/{sectionId}")
    suspend fun getSectionItems(@Path("sectionId") sectionId: Long): Response<List<ItemTemplateDto>>

    @POST("admin/checklist-sections/{sectionId}/items")
    suspend fun createSectionItem(@Path("sectionId") sectionId: Long, @Body item: ItemTemplateDto): Response<ItemTemplateDto>

    @PATCH("admin/checklist-items/{id}")
    suspend fun updateSectionItem(@Path("id") id: Long, @Body item: ItemTemplateDto): Response<ItemTemplateDto>

    @DELETE("admin/checklist-sections/{sectionId}/items/{itemId}")
    suspend fun deleteSectionItem(
        @Path("sectionId") sectionId: Long,
        @Path("itemId") itemId: Long
    ): Response<Unit>

    @PATCH("admin/checklist-items/section/{sectionId}/update-percentages")
    suspend fun updateItemPercentages(
        @Path("sectionId") sectionId: Long,
        @Body items: List<Map<String, Any>> // [{id, percentage}]
    ): Response<List<ItemTemplateDto>>

    @PATCH("admin/checklist-items/section/{sectionId}/distribute-percentages")
    suspend fun distributeItemPercentages(@Path("sectionId") sectionId: Long): Response<DistributeItemsResponse>

    @PATCH("admin/checklist-items/section/{sectionId}/reorder")
    suspend fun reorderItems(
        @Path("sectionId") sectionId: Long,
        @Body itemIds: List<Long>
    ): Response<List<ItemTemplateDto>>

    @PATCH("admin/checklist-items/{itemId}/move-to-section/{targetSectionId}")
    suspend fun moveItemToSection(
        @Path("itemId") itemId: Long,
        @Path("targetSectionId") targetSectionId: Long
    ): Response<ItemTemplateDto>

    @GET("/admin/checklist-sections/checklist/{checklistId}")
    suspend fun getChecklistSections(@Path("checklistId") checklistId: Long): Response<List<ChecklistSectionDto>>
}