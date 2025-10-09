package mx.checklist.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val STORES = "stores"
    const val HISTORY = "history"
    const val TEMPLATES = "templates/{storeCode}"
    const val RUN = "run/{runId}"
    
    // Admin routes
    const val ADMIN_TEMPLATES = "admin/templates"
    const val ADMIN_TEMPLATES_ADMIN = "admin/templates-admin"
    
    // Definición de rutas para usar en composable()
    // Formato requerido por Compose Navigation - NO MODIFICAR estas constantes
    const val ADMIN_TEMPLATE_FORM = "admin/templates/form/{templateId}"
    //  MODIFICADO: Eliminar sectionId - sistema plano
    const val ADMIN_ITEM_FORM = "admin/templates/{templateId}/items/form/{itemId}"
    const val ADMIN_SECTION_FORM = "admin/templates/{templateId}/sections/form/{sectionId}"
    const val ADMIN_ASSIGNMENTS = "admin/assignments"

    // Checklist routes
    const val CHECKLIST_STRUCTURE = "checklist_structure/{checklistId}"
    const val SECTION_ITEMS = "section_items/{sectionId}"

    // Funciones helper para rutas básicas
    fun templates(storeCode: String) = "templates/$storeCode"
    fun run(runId: Long) = "run/$runId"
    fun checklistStructure(checklistId: Long) = "checklist_structure/$checklistId"
    fun sectionItems(sectionId: Long) = "section_items/$sectionId"

    // Admin route helpers

    /**
     * Genera una ruta para el formulario de template con el ID especificado
     * @param templateId ID del template, o null para crear un nuevo template
     */
    fun adminTemplateForm(templateId: Long? = null): String {
        val templateIdValue = templateId ?: -1
        return "admin/templates/form/$templateIdValue"
    }
    
    /**
     * Genera una ruta para el formulario de ítem con el ID especificado
     * @param templateId ID del template al que pertenece el ítem
     * @param itemId ID del ítem, o null para crear un nuevo ítem
     */
    //  MODIFICADO: Eliminar sectionId - sistema plano con categorías
    fun adminItemForm(templateId: Long, itemId: Long? = null): String {
        val itemIdValue = itemId ?: -1
        return "admin/templates/$templateId/items/form/$itemIdValue"
    }
    
    /**
     * Genera una ruta para el formulario de sección con el ID especificado
     * @param templateId ID del template/checklist al que pertenece la sección
     * @param sectionId ID de la sección, o null para crear una nueva sección
     */
    fun adminSectionForm(templateId: Long, sectionId: Long? = null): String {
        val sectionIdValue = sectionId ?: -1
        return "admin/templates/$templateId/sections/form/$sectionIdValue"
    }
}
