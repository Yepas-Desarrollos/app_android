package mx.checklist.data.api.dto

data class StoreDto(
    val id: Long,
    val code: String,
    val name: String,
    val isActive: Boolean
)

data class TemplateDto(
    val id: Long,
    val name: String,
    val version: Int? = null,
    val scope: String? = null,
    val frequency: String? = null
)

data class CreateRunReq(
    val storeCode: String,
    val templateId: Long
)

data class RunRes(
    val id: Long,
    val templateId: Long,
    val storeId: Long,
    val status: String
)

data class RunItemDto(
    val id: Long,
    val runId: Long,
    val itemTemplateId: Long,
    val orderIndex: Int,
    val responseStatus: String? = null,
    val responseText: String? = null,
    val responseNumber: Double? = null
)

data class RespondReq(
    val responseStatus: String,
    val responseText: String? = null,
    val responseNumber: Double? = null
)
