package mx.checklist.data.api.dto

data class UpdateItemTemplateDto(
    val orderIndex: Int? = null,
    val title: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val percentage: Double? = null, // ✅ AGREGADO: soportar porcentaje en actualización
    val expectedType: String? = null,
    val config: Map<String, Any?>? = null
)