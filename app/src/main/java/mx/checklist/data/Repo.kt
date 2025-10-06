package mx.checklist.data

import android.util.Log
import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.*
import mx.checklist.data.auth.Authenticated
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File

class Repo(
    private val api: Api = ApiClient.api,
    private val tokenStore: TokenStore
) {
    // Campo de caché opcional
    private var cachedTemplates: List<TemplateDto>? = null

    suspend fun login(req: LoginReq): Authenticated {
        val res = api.login(req)
        
        Log.d("Repo", "🌐 Backend response - access_token: ${res.access_token?.take(20)}...")
        Log.d("Repo", "🌐 Backend response - roleCode: ${res.roleCode}")
        
        // Validar que tenemos los datos requeridos
        val token = res.access_token ?: throw IllegalStateException("Backend no devolvió access_token")
        val roleCode = res.roleCode ?: throw IllegalStateException("Backend no devolvió roleCode")
        
        // Crear objeto Authenticated con información básica por ahora
        val auth = Authenticated(
            token = token, 
            roleCode = roleCode
            // TODO: Agregar userId, email, fullName cuando el backend los devuelva
        )
        
        Log.d("Repo", "📦 Authenticated object - token: ${auth.token.take(20)}...")
        Log.d("Repo", "📦 Authenticated object - roleCode: ${auth.roleCode}")
        
        tokenStore.save(auth)
        Log.d("Repo", "💾 TokenStore.save() llamado con auth")
        
        ApiClient.setToken(res.access_token)

        // Limpia cachés por usuario
        cachedTemplates = null

        return auth
    }

    suspend fun login(email: String, password: String): Authenticated {
        return login(LoginReq(email.trim(), password))
    }

    suspend fun logout() {
        tokenStore.clear()
        cachedTemplates = null
        ApiClient.setToken(null)
    }

    suspend fun stores(): List<StoreDto> = api.stores()

    suspend fun templates(): List<TemplateDto> {
        // Si ya hay caché, retornarlo
        cachedTemplates?.let { return it }

        // Usar endpoint paginado y extraer solo los datos
        val response = api.templatesPaginated(page = 1, limit = 100)
        val list = response.data
        
        // No aplicar filtros por scope aquí. El backend ya filtra por rol.
        cachedTemplates = list

        // Log de diagnóstico
        Log.d("TEMPLATES", "count=${list.size} names=${list.joinToString { it.name }}")

        return list
    }

    // Templates paginados
    suspend fun templatesPaginated(page: Int = 1, limit: Int = 20): PaginatedTemplatesResponse {
        return api.templatesPaginated(page, limit)
    }

    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode.trim(), templateId))

    suspend fun runItems(runId: Long): List<RunItemDto> =
        api.runItems(runId).sortedBy { it.orderIndex }

    suspend fun respond(itemId: Long, status: String?, text: String?, number: Double?, barcode: String? = null): RunItemDto {
        val s = requireNotNull(status?.trim()?.takeIf { it.isNotEmpty() }) { "status requerido" }
        val t = text?.trim()?.takeUnless { it.isEmpty() }
        // Pasar el Double? directamente
        return api.respond(itemId, RespondReq(s, t, number, barcode))
    }

    suspend fun submit(runId: Long): RunRes = api.submit(runId)

    // Info run
    suspend fun runInfo(runId: Long): RunInfoDto = api.runInfo(runId)

    // Borradores / Historial
    suspend fun pendingRuns(limit: Int? = 20, all: Boolean? = false, storeCode: String? = null): List<RunSummaryDto> {
        // Usar endpoint paginado y extraer solo los datos
        val response = api.pendingRunsPaginated(page = 1, limit = limit ?: 20)
        return response.data
    }
    
    // Borradores paginados
    suspend fun pendingRunsPaginated(page: Int = 1, limit: Int = 20): PaginatedRunsResponse =
        api.pendingRunsPaginated(page, limit)

    suspend fun historyRuns(limit: Int? = 20, storeCode: String? = null): List<RunSummaryDto> {
        // Usar endpoint paginado y extraer solo los datos
        val response = api.historyRunsPaginated(page = 1, limit = limit ?: 20)
        return response.data
    }
    
    // Historial paginado
    suspend fun historyRunsPaginated(page: Int = 1, limit: Int = 20): PaginatedRunsResponse =
        api.historyRunsPaginated(page, limit)
        
    suspend fun deleteRun(runId: Long) { api.deleteRun(runId) }

    // === Evidencias ===
    suspend fun uploadAttachments(itemId: Long, files: List<File>): List<AttachmentDto> {
        val parts = files.map { file ->
            val media = "image/*".toMediaTypeOrNull()
            val body: RequestBody = file.asRequestBody(media)
            MultipartBody.Part.createFormData("files", file.name, body)
        }
        // Usar el cliente específico para uploads con timeouts más largos
        return ApiClient.uploadApi.uploadAttachments(itemId, parts)
    }

    // Added method
    suspend fun listAttachments(itemId: Long): List<AttachmentDto> {
        return api.listAttachments(itemId)
    }

    // Added method
    suspend fun deleteAttachment(itemId: Long, attachmentId: Int): DeleteAttachmentRes {
        return api.deleteAttachment(itemId, attachmentId)
    }

    // === ADMIN METHODS ===
    
    suspend fun adminGetTemplates(): List<AdminTemplateDto> {
        return api.adminGetTemplates()
    }

    // Admin templates paginados
    suspend fun adminGetTemplatesPaginated(page: Int = 1, limit: Int = 20): PaginatedAdminTemplatesResponse {
        return api.adminGetTemplatesPaginated(page, limit)
    }

    suspend fun adminCreateTemplate(request: CreateTemplateDto): CreateTemplateRes {
        // Limpiar caché de templates normales
        cachedTemplates = null
        return api.adminCreateTemplate(request)
    }

    suspend fun adminGetTemplate(templateId: Long): AdminTemplateDto {
        return api.adminGetTemplate(templateId)
    }

    // NUEVO: Método público para obtener estructura de template (AUDITOR/SUPERVISOR)
    // No requiere permisos de admin, solo autenticación JWT
    // El backend filtra automáticamente por scope del usuario
    suspend fun getTemplateStructure(templateId: Long): AdminTemplateDto {
        return api.getTemplateStructure(templateId)
    }

    suspend fun adminUpdateTemplate(templateId: Long, request: UpdateTemplateDto) {
        // Limpiar caché de templates normales
        cachedTemplates = null
        api.adminUpdateTemplate(templateId, request)
    }

    suspend fun adminDeleteTemplate(templateId: Long): DeleteRes {
        // Limpiar caché de templates normales
        cachedTemplates = null
        return api.adminDeleteTemplate(templateId)
    }

    suspend fun adminCreateItem(templateId: Long, request: CreateItemTemplateDto): CreateItemTemplateRes {
        return api.adminCreateItem(templateId, request)
    }

    suspend fun adminUpdateItem(templateId: Long, itemId: Long, request: UpdateItemTemplateDto) {
        api.adminUpdateItem(templateId, itemId, request)
    }

    suspend fun adminDeleteItem(templateId: Long, itemId: Long): DeleteRes {
        return api.adminDeleteItem(templateId, itemId)
    }

    suspend fun adminUpdateTemplateStatus(templateId: Long, isActive: Boolean): TemplateStatusRes {
        // Limpiar caché de templates normales
        cachedTemplates = null
        return api.adminUpdateTemplateStatus(templateId, UpdateTemplateStatusDto(isActive))
    }

    suspend fun adminForceDeleteRun(runId: Long): ForceDeleteRunRes {
        return api.adminForceDeleteRun(runId)
    }

    // === ASSIGNMENT METHODS ===
    
    /**
     * Obtener usuarios que pueden ser asignados (AUDITOR/SUPERVISOR)
     */
    suspend fun getAssignableUsers(): List<AssignableUserDto> {
        return api.getAssignableUsers()
    }
    
    /**
     * Obtener resumen de asignaciones por área
     */
    suspend fun getAssignmentSummary(): List<AssignmentSummaryDto> {
        val response = api.getAssignmentSummary()
        return response.data
    }
    
    /**
     * Asignar usuario a sectores específicos
     */
    suspend fun assignUserToSectors(userId: Long, sectors: List<Int>): AssignmentResponse {
        return api.assignUserToSectors(
            AssignUserToSectorsRequest(
                userId = userId.toString(),
                sectors = sectors
            )
        )
    }
    
    /**
     * Obtener tiendas asignadas a un usuario
     */
    suspend fun getUserAssignedStores(userId: Long): List<AssignedStoreDto> {
        return api.getUserAssignedStores(userId.toString())
    }

    /** Obtener sectores disponibles */
    suspend fun getAssignmentSectors(): List<Int> {
        return api.getAssignmentSectors()
    }

    private fun <T> Response<T>.requireBody(): T {
        if (isSuccessful) return body() ?: throw IllegalStateException("Respuesta sin cuerpo")
        throw HttpException(this)
    }

    // === ESTRUCTURA CHECKLIST (Secciones & Items) ===
    suspend fun getSections(checklistId: Long): List<SectionTemplateDto> = api.getSections(checklistId).requireBody()
    suspend fun createSection(checklistId: Long, section: SectionTemplateCreateDto): SectionTemplateDto = api.createSection(checklistId, section).requireBody()
    suspend fun updateSection(id: Long, section: SectionTemplateUpdateDto): SectionTemplateDto {
        return api.updateSection(id, section).requireBody()
    }
    suspend fun deleteSection(id: Long) { api.deleteSection(id).requireBody() }
    suspend fun updateSectionPercentages(checklistId: Long, sections: List<mx.checklist.data.api.dto.SectionPercentageUpdateDto>): List<SectionTemplateDto> =
        api.updateSectionPercentages(checklistId, mx.checklist.data.api.dto.SectionPercentagesPayload(sections)).requireBody()

    // Implementación manual de distribución de porcentajes para secciones
    suspend fun distributeSectionPercentages(checklistId: Long): List<SectionTemplateDto> {
        // Obtener las secciones actuales
        val currentSections = getSections(checklistId)

        if (currentSections.isEmpty()) {
            return emptyList()
        }

        // Calcular porcentaje equitativo
        val equalPercentage = 100.0 / currentSections.size

        // Crear lista de actualizaciones
        val updates = currentSections.map { section ->
            mx.checklist.data.api.dto.SectionPercentageUpdateDto(
                id = section.id ?: 0L,
                percentage = equalPercentage
            )
        }

        // Usar el endpoint de actualización existente
        return updateSectionPercentages(checklistId, updates)
    }

    suspend fun reorderSections(checklistId: Long, sectionIds: List<Long>): List<SectionTemplateDto> = api.reorderSections(checklistId, sectionIds).requireBody()

    suspend fun getSectionItems(sectionId: Long): List<ItemTemplateDto> = api.getSectionItems(sectionId).requireBody()
    suspend fun createSectionItem(sectionId: Long, item: ItemTemplateDto): ItemTemplateDto = api.createSectionItem(sectionId, item).requireBody()
    suspend fun updateSectionItem(id: Long, item: ItemTemplateDto): ItemTemplateDto = api.updateSectionItem(id, item).requireBody()
    suspend fun deleteSectionItem(sectionId: Long, itemId: Long) {
        api.deleteSectionItem(sectionId, itemId).requireBody()
    }
    suspend fun updateItemPercentages(sectionId: Long, items: List<Map<String, Any>>): List<ItemTemplateDto> = api.updateItemPercentages(sectionId, items).requireBody()
    suspend fun distributeItemPercentages(sectionId: Long): List<ItemTemplateDto> {
        val response = api.distributeItemPercentages(sectionId).requireBody()
        return response.items ?: emptyList()
    }
    suspend fun reorderItems(sectionId: Long, itemIds: List<Long>): List<ItemTemplateDto> = api.reorderItems(sectionId, itemIds).requireBody()
    suspend fun moveItemToSection(itemId: Long, targetSectionId: Long): ItemTemplateDto = api.moveItemToSection(itemId, targetSectionId).requireBody()

    suspend fun getChecklistSections(checklistId: Long): List<ChecklistSectionDto> =
        api.getChecklistSections(checklistId).requireBody()
}
