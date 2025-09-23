package mx.checklist.data.api.dto

// Respuesta al crear template
data class CreateTemplateRes(
    val id: Long,
    val name: String,
    val createdAt: String? = null
)

// Respuesta al crear item template
data class CreateItemTemplateRes(
    val id: Long,
    val templateId: Long,
    val orderIndex: Int,
    val title: String,
    val category: String? = null,
    val subcategory: String? = null,
    val expectedType: String,
    val config: Map<String, Any?>? = null,
    val createdAt: String? = null
)

// Para las operaciones de eliminación
data class DeleteRes(
    val success: Boolean,
    val message: String? = null
)

// Para actualizar status de template
data class UpdateTemplateStatusDto(
    val isActive: Boolean
)

// Respuesta al actualizar status de template
data class TemplateStatusRes(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val message: String
)

// Respuesta al eliminar run por fuerza
data class ForceDeleteRunRes(
    val success: Boolean,
    val message: String,
    val deletedItems: Int
)