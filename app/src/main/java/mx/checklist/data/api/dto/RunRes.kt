package mx.checklist.data.api.dto

data class RunRes(
    val id: Long,
    val templateId: Long,
    val storeId: Long,
    val status: String
)
