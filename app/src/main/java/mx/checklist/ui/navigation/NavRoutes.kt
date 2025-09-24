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
    const val ADMIN_TEMPLATE_FORM = "admin/templates/form?templateId={templateId}"
    const val ADMIN_ITEM_FORM = "admin/templates/{templateId}/items/form?itemId={itemId}"
    const val ADMIN_ASSIGNMENTS = "admin/assignments"

    fun templates(storeCode: String) = "templates/$storeCode"
    fun run(runId: Long) = "run/$runId"
    
    // Admin route helpers
    fun adminTemplateForm(templateId: Long? = null) = 
        "admin/templates/form?templateId=${templateId ?: -1}"
    
    fun adminItemForm(templateId: Long, itemId: Long? = null) = 
        "admin/templates/$templateId/items/form?itemId=${itemId ?: -1}"
}
