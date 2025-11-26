package mx.checklist.data.repository

import mx.checklist.data.api.Api
import mx.checklist.data.api.dto.*
import retrofit2.Response
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: Api
) {
    suspend fun getTemplates(): List<AdminTemplateDto> {
        return api.adminGetTemplates()
    }

    suspend fun getTemplatesPaginated(page: Int = 1, limit: Int = 20): PaginatedAdminTemplatesResponse {
        val allTemplates = api.adminGetTemplates()
        
        // Simulación de paginación en cliente (como estaba en Repo.kt)
        val startIndex = (page - 1) * limit
        val endIndex = minOf(startIndex + limit, allTemplates.size)
        val pageData = allTemplates.subList(startIndex, endIndex)
        val totalPages = (allTemplates.size + limit - 1) / limit
        val hasMore = page < totalPages

        return PaginatedAdminTemplatesResponse(
            data = pageData,
            pagination = PaginationDto(page, limit, allTemplates.size, totalPages, hasMore)
        )
    }

    suspend fun createTemplate(request: CreateTemplateDto): CreateTemplateRes {
        return api.adminCreateTemplate(request)
    }

    suspend fun getTemplate(templateId: Long): AdminTemplateDto {
        return api.adminGetTemplate(templateId)
    }
    
    suspend fun getTemplateStructure(templateId: Long): AdminTemplateDto {
        return api.getTemplateStructure(templateId)
    }

    suspend fun updateTemplate(templateId: Long, request: UpdateTemplateDto) {
        api.adminUpdateTemplate(templateId, request)
    }

    suspend fun deleteTemplate(templateId: Long): DeleteRes {
        return api.adminDeleteTemplate(templateId)
    }

    suspend fun updateTemplateStatus(templateId: Long, isActive: Boolean): TemplateStatusRes {
        return api.adminUpdateTemplateStatus(templateId, UpdateTemplateStatusDto(isActive))
    }

    suspend fun forceDeleteRun(runId: Long): ForceDeleteRunRes {
        return api.adminForceDeleteRun(runId)
    }

    // === Items & Sections Management ===

    suspend fun createItem(templateId: Long, request: CreateItemTemplateDto): CreateItemTemplateRes {
        return api.adminCreateItem(templateId, request)
    }

    suspend fun updateItem(templateId: Long, itemId: Long, request: UpdateItemTemplateDto) {
        api.adminUpdateItem(templateId, itemId, request)
    }

    suspend fun deleteItem(templateId: Long, itemId: Long): DeleteRes {
        return api.adminDeleteItem(templateId, itemId)
    }

    suspend fun createSection(checklistId: Long, section: SectionTemplateCreateDto): SectionTemplateDto = 
        api.createSection(checklistId, section).requireBody()

    suspend fun updateSection(id: Long, section: SectionTemplateUpdateDto): SectionTemplateDto = 
        api.updateSection(id, section).requireBody()

    suspend fun deleteSection(id: Long) { 
        api.deleteSection(id).requireBody() 
    }

    suspend fun reorderSections(checklistId: Long, sectionIds: List<Long>): List<SectionTemplateDto> = 
        api.reorderSections(checklistId, sectionIds).requireBody()

    suspend fun createSectionItem(sectionId: Long, item: ItemTemplateDto): ItemTemplateDto = 
        api.createSectionItem(sectionId, item).requireBody()

    suspend fun updateSectionItem(id: Long, item: ItemTemplateDto): ItemTemplateDto = 
        api.updateSectionItem(id, item).requireBody()

    suspend fun deleteSectionItem(sectionId: Long, itemId: Long) {
        api.deleteSectionItem(sectionId, itemId).requireBody()
    }

    suspend fun reorderItems(sectionId: Long, itemIds: List<Long>): List<ItemTemplateDto> = 
        api.reorderItems(sectionId, itemIds).requireBody()

    suspend fun moveItemToSection(itemId: Long, targetSectionId: Long): ItemTemplateDto = 
        api.moveItemToSection(itemId, targetSectionId).requireBody()

    suspend fun updateSectionPercentages(checklistId: Long, sections: List<SectionPercentageUpdateDto>): List<SectionTemplateDto> =
        api.updateSectionPercentages(checklistId, SectionPercentagesPayload(sections)).requireBody()

    suspend fun distributeSectionPercentages(checklistId: Long): List<SectionTemplateDto> {
        val currentSections = api.getSections(checklistId).requireBody()
        if (currentSections.isEmpty()) return emptyList()

        val equalPercentage = 100.0 / currentSections.size
        val updates = currentSections.map { section ->
            SectionPercentageUpdateDto(id = section.id ?: 0L, percentage = equalPercentage)
        }
        return api.updateSectionPercentages(checklistId, SectionPercentagesPayload(updates)).requireBody()
    }

    suspend fun getSectionItems(sectionId: Long): List<ItemTemplateDto> = 
        api.getSectionItems(sectionId).requireBody()

    suspend fun updateItemPercentages(sectionId: Long, items: List<Map<String, Any>>): List<ItemTemplateDto> = 
        api.updateItemPercentages(sectionId, items).requireBody()

    suspend fun distributeItemPercentages(sectionId: Long): List<ItemTemplateDto> {
        val response = api.distributeItemPercentages(sectionId).requireBody()
        return response.items ?: emptyList()
    }

    private fun <T> Response<T>.requireBody(): T {
        if (isSuccessful) return body() ?: throw IllegalStateException("Respuesta sin cuerpo")
        throw HttpException(this)
    }
}
