package mx.checklist.data.auth

object AuthState {
    @Volatile
    var token: String? = null

    @Volatile
    var roleCode: String? = null
}
