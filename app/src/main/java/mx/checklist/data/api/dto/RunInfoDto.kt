package mx.checklist.data.api.dto

data class RunInfoDto(
    val id: Long,
    val templateId: Long,
    val storeId: Long,
    val status: String,          // PENDING | ... | SUBMITTED
    val templateName: String? = null,
    val storeCode: String? = null
)
