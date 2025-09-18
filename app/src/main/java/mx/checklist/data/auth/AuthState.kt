package mx.checklist.data.auth

data class Authenticated(
    val token: String,
    val roleCode: String? = null
)

object AuthState {
    @Volatile
    var token: String? = null

    @Volatile
    var roleCode: String? = null
}
