package mx.checklist.data.api.dto

data class CreateTemplateDto(
    val name: String,
    val items: List<CreateItemTemplateDto> = emptyList()
)