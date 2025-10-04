package mx.checklist.data.api.dto

data class SectionTemplateCreateDto(
    val name: String,
    val percentage: Double,
    val orderIndex: Int
)

