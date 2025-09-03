package mx.checklist.ui.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val STORES = "stores"
    const val TEMPLATES = "templates/{storeCode}"
    const val RUN = "run/{runId}"

    fun templates(storeCode: String) = "templates/$storeCode"
    fun run(runId: Long) = "run/$runId"
}
