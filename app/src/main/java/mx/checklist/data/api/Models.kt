package mx.checklist.data.api
data class TokenRes(val access_token: String)
data class StoreDto(val id: Long, val code: String, val name: String)
data class TemplateDto(val id: Long, val name: String, val version: Int, val scope: String, val frequency: String)
data class RunDto(val id: Long, val templateId: Long, val storeId: Long, val status: String)
data class ItemTemplateDto(val category: String?, val subcategory: String?, val title: String, val expectedType: String, val barcodeHint: String?)
data class ItemDto(
  val id: Long, val orderIndex: Int,
  val itemTemplate: ItemTemplateDto,
  val responseStatus: String? = null, val responseText: String? = null, val responseNumber: Double? = null
)
data class LoginReq(val email: String, val password: String)
data class CreateRunReq(val storeCode: String, val templateId: Long)
data class RespondReq(
  val responseStatus: String? = null, val responseText: String? = null, val responseNumber: Double? = null, val scannedBarcode: String? = null
)
