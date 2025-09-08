package mx.checklist.data.api.dto

data class ItemTemplateDto(
    val id: Long,
    val orderIndex: Int,
    val title: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val expectedType: String? = null,
    // Config flexible: Gson lo mapeará a LinkedTreeMap
    val config: Map<String, Any?>? = null
)
