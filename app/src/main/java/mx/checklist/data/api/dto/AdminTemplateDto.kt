package mx.checklist.data.api.dto

data class AdminTemplateDto(
    val id: Long,
    val name: String,
    val scope: String? = null,
    val version: Int? = null,
    val frequency: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val items: List<ItemTemplateDto> = emptyList()
)