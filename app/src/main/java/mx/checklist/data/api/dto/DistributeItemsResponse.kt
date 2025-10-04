package mx.checklist.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DistributeItemsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "itemsUpdated") val itemsUpdated: Int,
    @Json(name = "items") val items: List<ItemTemplateDto>? = null
)
