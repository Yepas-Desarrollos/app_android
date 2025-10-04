package mx.checklist.data.api.dto

// DTO unificado compatible con endpoints antiguos (sin percentage) y nuevos (con percentage)
// Si el backend no envía 'percentage', Moshi usará el valor por defecto 0.0 evitando JsonDataException.

data class ItemTemplateDto(
    val id: Long? = null,
    val sectionId: Long? = null,
    val title: String? = null,
    val percentage: Double? = null, // Ahora acepta null
    val category: String? = null,
    val subcategory: String? = null,
    val expectedType: String? = null,
    val orderIndex: Int = 0,
    val config: Map<String, Any?>? = null
)
