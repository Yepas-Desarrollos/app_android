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

    /**
     * Envía la respuesta de un ítem:
     * - status: requerido
     * - text: opcional
     * - number: opcional (Double?)
     */
    suspend fun respond(
        itemId: Long,
        status: String?,
        text: String?,
        number: Double?
    ): RunItemDto {
        val s = requireNotNull(status?.trim()?.takeIf { it.isNotEmpty() }) {
            "status requerido"
        }

        val t: String? = text?.trim()?.takeUnless { it.isEmpty() }

        // Sanitiza NaN/Infinity → null, para no romper el JSON
        val n: Double? = number?.let { d ->
            if (d.isNaN() || d.isInfinite()) null else d
        }

        val body = RespondReq(
            responseStatus = s,
            responseText = t,
            responseNumber = n
        )

        return api.respond(itemId, body)
    }

    suspend fun submit(runId: Long): RunRes = api.submit(runId)
}
