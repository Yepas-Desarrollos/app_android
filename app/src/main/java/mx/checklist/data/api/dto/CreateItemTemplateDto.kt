package mx.checklist.data.api.dto

import com.squareup.moshi.Json

data class CreateItemTemplateDto(
    @Json(name = "sectionId") val sectionId: Long? = null, // Será omitido del JSON si es null
    val orderIndex: Int,
    val title: String,
    val category: String? = null,
    val subcategory: String? = null,
    val percentage: Double? = null,
    val expectedType: String, // BOOLEAN, SINGLE_CHOICE, MULTISELECT, SCALE, NUMBER, TEXT, PHOTO, BARCODE
    val config: Map<String, Any?>? = null
)