package mx.checklist.data.api.dto

data class RunItemDto(
    val id: Long,
    val runId: Long,
    val itemTemplateId: Long,
    val orderIndex: Int,
    val responseStatus: String? = null,
    val responseText: String? = null,
    val responseNumber: Double? = null,
    val scannedBarcode: String? = null,
    val respondedAt: String? = null,
    val itemTemplate: ItemTemplateDto? = null,
    val attachments: List<AttachmentDto>? = emptyList()
)
