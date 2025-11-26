package mx.checklist.data.repository

import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunRepository @Inject constructor(
    private val api: Api
) {
    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode.trim(), templateId))

    suspend fun getRunItems(runId: Long): List<RunItemDto> =
        api.runItems(runId).sortedBy { it.orderIndex }

    suspend fun respond(itemId: Long, status: String?, text: String?, number: Double?, barcode: String? = null): RunItemDto {
        val s = requireNotNull(status?.trim()?.takeIf { it.isNotEmpty() }) { "status requerido" }
        val t = text?.trim()?.takeUnless { it.isEmpty() }
        return api.respond(itemId, RespondReq(s, t, number, barcode))
    }

    suspend fun submitRun(runId: Long): RunRes = api.submit(runId)

    suspend fun getRunInfo(runId: Long): RunInfoDto = api.runInfo(runId)

    suspend fun deleteRun(runId: Long) { api.deleteRun(runId) }

    // === History & Pending ===

    suspend fun getPendingRuns(limit: Int = 20): List<RunSummaryDto> {
        val response = api.pendingRunsPaginated(page = 1, limit = limit)
        return response.data
    }
    
    suspend fun getPendingRunsPaginated(page: Int = 1, limit: Int = 20): PaginatedRunsResponse =
        api.pendingRunsPaginated(page, limit)

    suspend fun getHistoryRuns(limit: Int = 20): List<RunSummaryDto> {
        val response = api.historyRunsPaginated(page = 1, limit = limit)
        return response.data
    }
    
    suspend fun getHistoryRunsPaginated(page: Int = 1, limit: Int = 20): PaginatedRunsResponse =
        api.historyRunsPaginated(page, limit)

    // === Attachments ===

    suspend fun uploadAttachments(itemId: Long, files: List<File>): List<AttachmentDto> {
        val parts = files.map { file ->
            val media = "image/*".toMediaTypeOrNull()
            val body: RequestBody = file.asRequestBody(media)
            MultipartBody.Part.createFormData("files", file.name, body)
        }
        return ApiClient.uploadApi.uploadAttachments(itemId, parts)
    }

    suspend fun listAttachments(itemId: Long): List<AttachmentDto> {
        return api.listAttachments(itemId)
    }

    suspend fun deleteAttachment(itemId: Long, attachmentId: Int): DeleteAttachmentRes {
        return api.deleteAttachment(itemId, attachmentId)
    }
}
