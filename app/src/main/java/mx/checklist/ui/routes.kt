package mx.checklist.ui

/**
 * API de rutas por constantes + helpers (lo simple).
 */
object Routes {
    const val Login = "login"
    const val Stores = "stores"
    const val Templates = "templates/{storeCode}"
    const val Items = "items/{runId}?storeCode={storeCode}"

    fun template(storeCode: String) = "templates/$storeCode"
    fun items(runId: Long, storeCode: String) = "items/$runId?storeCode=$storeCode"
}

/**
 * API alternativa tipo sealed class con .route
 * (para compatibilidad con código viejo que usa Route.X.route).
 */
sealed class Nav(val route: String) {
    data object Login : Nav(Routes.Login)
    data object Stores : Nav(Routes.Stores)
    data object Templates : Nav(Routes.Templates)
    data object Items : Nav(Routes.Items)
}

/**
 * Alias opcional: si en algún sitio referías "Route.X",
 * podrás seguir usando "Route" como alias de "Routes".
 */
typealias Route = Routes
