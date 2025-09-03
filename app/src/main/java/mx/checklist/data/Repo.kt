package mx.checklist.data

import mx.checklist.data.api.Api
// Se eliminó la importación con asterisco y se reemplazó por las siguientes:
import mx.checklist.data.api.dto.LoginReq // La correcta, del paquete DTO
import mx.checklist.data.api.dto.LoginRes
import mx.checklist.data.api.dto.StoreDto
import mx.checklist.data.api.dto.TemplateDto
import mx.checklist.data.api.dto.CreateRunReq
import mx.checklist.data.api.dto.RunRes
import mx.checklist.data.api.dto.RunItemDto
import mx.checklist.data.api.dto.RespondReq

class Repo(
    private val api: Api,
    private val tokenStore: TokenStore
) {
    suspend fun login(email: String, password: String): String {
        val res = api.login(LoginReq(email, password)) // LoginReq ahora es explícito
        tokenStore.saveToken(res.accessToken)
        return res.accessToken
    }
    suspend fun stores(): List<StoreDto> = api.getStores()
    suspend fun templates(): List<TemplateDto> = api.getTemplates()
    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode, templateId))
    suspend fun runItems(runId: Long): List<RunItemDto> = api.getRunItems(runId)
    suspend fun respond(itemId: Long, status: String, text: String? = null, number: Double? = null): RunItemDto =
        api.respondItem(itemId, RespondReq(status, text, number))
    suspend fun submit(runId: Long): RunRes = api.submitRun(runId)
}
