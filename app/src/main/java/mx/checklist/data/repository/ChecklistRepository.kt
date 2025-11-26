package mx.checklist.data.repository

import android.util.Log
import mx.checklist.data.api.Api
import mx.checklist.data.api.dto.*
import retrofit2.Response
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecklistRepository @Inject constructor(
    private val api: Api
) {
    // Cache simple para templates
    private var cachedTemplates: List<TemplateDto>? = null

    suspend fun getTemplates(forceRefresh: Boolean = false): List<TemplateDto> {
        if (!forceRefresh && cachedTemplates != null) {
            return cachedTemplates!!
        }

        val response = api.templatesPaginated(page = 1, limit = 100)
        val list = response.data
        cachedTemplates = list
        
        Log.d("ChecklistRepo", "Templates loaded: count=${list.size}")
        return list
    }

    suspend fun getTemplatesPaginated(page: Int = 1, limit: Int = 20): PaginatedTemplatesResponse {
        return api.templatesPaginated(page, limit)
    }

    suspend fun getStores(): List<StoreDto> = api.stores()

    // === Structure ===

    suspend fun getChecklistSections(checklistId: Long): List<ChecklistSectionDto> =
        api.getChecklistSections(checklistId).requireBody()

    suspend fun getSections(checklistId: Long): List<SectionTemplateDto> = 
        api.getSections(checklistId).requireBody()

    suspend fun getSectionItems(sectionId: Long): List<ItemTemplateDto> = 
        api.getSectionItems(sectionId).requireBody()

    private fun <T> Response<T>.requireBody(): T {
        if (isSuccessful) return body() ?: throw IllegalStateException("Respuesta sin cuerpo")
        throw HttpException(this)
    }
    
    fun clearCache() {
        cachedTemplates = null
    }
}
