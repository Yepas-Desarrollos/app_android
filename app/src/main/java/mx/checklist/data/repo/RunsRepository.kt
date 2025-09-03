package mx.checklist.data.repo

import mx.checklist.data.api.Api
import mx.checklist.data.api.dto.*

class RunsRepository(private val api: Api) {
    suspend fun stores(): List<StoreDto> = api.getStores()
    suspend fun templates(): List<TemplateDto> = api.getTemplates()
    suspend fun createRun(storeCode: String, templateId: Long): RunRes =
        api.createRun(CreateRunReq(storeCode, templateId))
    suspend fun respond(itemId: Long, status: String, text: String? = null, number: Double? = null): RunItemDto =
        api.respondItem(itemId, RespondReq(status, text, number))
}
