package mx.checklist.data.api.dto

data class CreateTemplateDto(
    val name: String,
    val description: String? = null,
    val items: List<CreateItemTemplateDto> = emptyList()
)