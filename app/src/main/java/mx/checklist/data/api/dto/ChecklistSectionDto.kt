package mx.checklist.data.api.dto

import mx.checklist.data.api.dto.ItemTemplateDto

// Representa una sección real de checklist (no solo del template)
data class ChecklistSectionDto(
    val id: Long? = null,
    val checklistId: Long? = null,
    val name: String,
    val percentage: Double,
    val orderIndex: Int,
    val items: List<ItemTemplateDto> = emptyList()
)

