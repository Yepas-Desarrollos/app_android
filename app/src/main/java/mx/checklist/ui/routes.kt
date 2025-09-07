package mx.checklist.ui

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Stores = "stores"
    const val History = "history" // ⬅️ nuevo
    const val Templates = "templates/{storeCode}"
    const val Items = "items/{runId}?storeCode={storeCode}&templateName={templateName}"

    fun template(storeCode: String) = "templates/$storeCode"
    fun items(runId: Long, storeCode: String, templateName: String? = null): String {
        val tn = templateName ?: ""
        return "items/$runId?storeCode=$storeCode&templateName=$tn"
    }
}

sealed class Nav(val route: String) {
    data object Login : Nav(Routes.Login)
    data object Home : Nav(Routes.Home)
    data object Stores : Nav(Routes.Stores)
    data object History : Nav(Routes.History)
    data object Templates : Nav(Routes.Templates)
    data object Items : Nav(Routes.Items)
}

typealias Route = Routes
