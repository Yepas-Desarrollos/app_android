package mx.checklist.data.api.dto

data class RunSummaryDto(
    val id: Long,
    val templateId: Long,
    val storeId: Long,
    val status: String,
    val templateName: String?,
    val storeCode: String?,
    val totalCount: Int,
    val answeredCount: Int,
    val updatedAt: String // ISO datetime del backend
)
