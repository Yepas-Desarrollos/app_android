package mx.checklist.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SectionTemplateUpdateDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String,
    @Json(name = "percentage") val percentage: Double,
    @Json(name = "orderIndex") val orderIndex: Int
)
