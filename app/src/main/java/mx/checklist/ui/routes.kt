package mx.checklist.ui

/**
 * Rutas únicas de la app + helpers.
 */
object Routes {
    const val Home = "home"
    const val Login = "login"
    const val Stores = "stores"
    const val Templates = "templates/{storeCode}"
    const val Items = "items/{runId}?storeCode={storeCode}"

    fun template(storeCode: String) = "templates/$storeCode"
    fun items(runId: Long, storeCode: String) = "items/$runId?storeCode=$storeCode"
}

/**
 * (Opcional) API alternativa tipo sealed class para compatibilidad.
 */
sealed class Nav(val route: String) {
    data object Home : Nav(Routes.Home)
    data object Login : Nav(Routes.Login)
    data object Stores : Nav(Routes.Stores)
    data object Templates : Nav(Routes.Templates)
    data object Items : Nav(Routes.Items)
}

typealias Route = Routes
