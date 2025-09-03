package mx.checklist.ui
sealed class Route(val r: String) {
  data object Login : Route("login")
  data object Stores : Route("stores")
  data object Templates : Route("templates/{storeCode}") { fun path(storeCode: String) = "templates/$storeCode" }
  data object Items : Route("items/{runId}/{storeCode}") { fun path(runId: Long, storeCode: String) = "items/$runId/$storeCode" }
}
