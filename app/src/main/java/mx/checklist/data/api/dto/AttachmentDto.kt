package mx.checklist.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import java.lang.Exception
import kotlin.jvm.Transient

@JsonClass(generateAdapter = true)
data class AttachmentDto(
    @Json(name = "id") val id: Int,
    @Json(name = "type") val type: String, // 'PHOTO', 'SIGNATURE', etc.
    @Json(name = "url") val url: String,
    @Json(name = "createdAt") val createdAt: String,
    @Transient val localUri: String? = null // No se serializa, solo para uso en UI
)

typealias AttachmentsUploadResultDto = List<AttachmentDto>

// Added for the delete endpoint response
@JsonClass(generateAdapter = true)
data class DeleteAttachmentRes(
    @Json(name = "deleted") val deleted: Boolean,
    @Json(name = "id") val id: Int
)
