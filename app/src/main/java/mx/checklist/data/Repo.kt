package mx.checklist.data

import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.*

class Repo(
    private val api: Api = ApiClient.api,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String) {
        val res = api.login(LoginReq(email, password))
        tokenStore.saveToken(res.access_token)
        ApiClient.setToken(res.access_token)
    }

    suspend fun stores(): List<StoreDto> = api.stores()

    suspend fun templates(): List<TemplateDto> = api.templates()

    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode, templateId))

    suspend fun runItems(runId: Long): List<RunItemDto> = api.runItems(runId)

    suspend fun respond(itemId: Long, status: String?, text: String?, number: Double?): RunItemDto =
        api.respond(itemId, RespondReq(status, text, number))

    suspend fun submit(runId: Long): RunRes = api.submit(runId)
}
