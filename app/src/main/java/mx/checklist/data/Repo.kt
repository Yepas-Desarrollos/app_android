package mx.checklist.data

import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.*

class Repo(
    private val api: Api = ApiClient.api,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String) {
        val res = api.login(LoginReq(email.trim(), password))
        tokenStore.saveToken(res.access_token)
        ApiClient.setToken(res.access_token)
    }

    suspend fun stores(): List<StoreDto> = api.stores()
    suspend fun templates(): List<TemplateDto> = api.templates()

    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode.trim(), templateId))

    suspend fun runItems(runId: Long): List<RunItemDto> = api.runItems(runId)

    suspend fun respond(itemId: Long, status: String?, text: String?, number: Double?): RunItemDto {
        val s = requireNotNull(status?.trim()?.takeIf { it.isNotEmpty() }) { "status requerido" }
        val t = text?.trim()?.takeUnless { it.isEmpty() }
        val n = number?.let { d -> if (d.isNaN() || d.isInfinite()) null else d }
        return api.respond(itemId, RespondReq(s, t, n))
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
}
