package mx.checklist.data.api.dto

data class CreateItemTemplateDto(
    val orderIndex: Int,
    val title: String,
    val category: String? = null,
    val subcategory: String? = null,
    val expectedType: String, // BOOLEAN, SINGLE_CHOICE, MULTISELECT, SCALE, NUMBER, TEXT, PHOTO, BARCODE
    val config: Map<String, Any?>? = null
)