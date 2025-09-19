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
        val auth = Authenticated(token = res.access_token, roleCode = res.roleCode)
        tokenStore.save(auth)
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

        val list = api.templates()
        // No aplicar filtros por scope aquí. El backend ya filtra por rol.
        cachedTemplates = list

        // Log de diagnóstico
        Log.d("TEMPLATES", "count=${list.size} names=${list.joinToString { it.name }}")

        return list
    }

    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode.trim(), templateId))

    suspend fun runItems(runId: Long): List<RunItemDto> = api.runItems(runId)

    suspend fun respond(itemId: Long, status: String?, text: String?, number: Double?): RunItemDto {
        val s = requireNotNull(status?.trim()?.takeIf { it.isNotEmpty() }) { "status requerido" }
        val t = text?.trim()?.takeUnless { it.isEmpty() }
        // Convert the Double? to Int?
        val nAsInt = number?.let { d -> if (d.isNaN() || d.isInfinite()) null else d.toInt() }
        return api.respond(itemId, RespondReq(s, t, nAsInt)) // Pass the Int? version
    }

    suspend fun submit(runId: Long): RunRes = api.submit(runId)

    // Info run
    suspend fun runInfo(runId: Long): RunInfoDto = api.runInfo(runId)

    // Borradores / Historial
    suspend fun pendingRuns(limit: Int? = 20, all: Boolean? = false, storeCode: String? = null): List<RunSummaryDto> =
        api.pendingRuns(limit, all, storeCode)

    suspend fun historyRuns(limit: Int? = 20, storeCode: String? = null): List<RunSummaryDto> =
        api.historyRuns(limit, storeCode)

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
}
