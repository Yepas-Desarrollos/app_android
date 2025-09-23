package mx.checklist.data

import android.util.Log
import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.*
import mx.checklist.data.auth.Authenticated
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class Repo(
    private val api: Api = ApiClient.api,
    private val tokenStore: TokenStore
) {
    // Campo de caché opcional
    private var cachedTemplates: List<TemplateDto>? = null

    suspend fun login(req: LoginReq): Authenticated {
        val res = api.login(req)
        
        Log.d("Repo", "🌐 Backend response - access_token: ${res.access_token.take(20)}...")
        Log.d("Repo", "🌐 Backend response - roleCode: ${res.roleCode}")
        
        val auth = Authenticated(token = res.access_token, roleCode = res.roleCode)
        
        Log.d("Repo", "📦 Authenticated object - token: ${auth.token?.take(20)}...")
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

    suspend fun runItems(runId: Long): List<RunItemDto> = api.runItems(runId)

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
            val body: RequestBody = RequestBody.create(media, file)
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
}
