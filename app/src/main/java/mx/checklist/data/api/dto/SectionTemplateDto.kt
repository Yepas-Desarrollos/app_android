package mx.checklist.data.api.dto

// Representa una sección dentro de un checklist template
// Cada sección contiene una lista de items y un porcentaje que debe sumarse a 100% junto con las demás secciones

data class SectionTemplateDto(
    val id: Long? = null,
    val name: String,
    val percentage: Double? = null, // Cambiado a nullable para consistencia
    val orderIndex: Int,
    val items: List<ItemTemplateDto> = emptyList()
)

// DTOs auxiliares para actualizar porcentajes masivamente
// Usados por ViewModel y llamadas al backend

data class SectionPercentage(val id: Long, val percentage: Double)
data class ItemPercentage(val id: Long, val percentage: Double)
