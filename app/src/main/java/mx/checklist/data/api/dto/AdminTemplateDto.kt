package mx.checklist.data.api.dto

data class AdminTemplateDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val items: List<ItemTemplateDto> = emptyList()
)