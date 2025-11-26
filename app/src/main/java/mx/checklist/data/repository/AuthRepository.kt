package mx.checklist.data.repository

import mx.checklist.data.TokenStore
import mx.checklist.data.api.Api
import mx.checklist.data.api.ApiClient
import mx.checklist.data.api.dto.LoginReq
import mx.checklist.data.auth.Authenticated
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: Api,
    private val tokenStore: TokenStore
) {
    suspend fun login(req: LoginReq): Authenticated {
        val res = api.login(req)
        
        val token = res.access_token ?: throw IllegalStateException("Backend no devolvió access_token")
        val roleCode = res.roleCode ?: throw IllegalStateException("Backend no devolvió roleCode")
        
        val auth = Authenticated(
            token = token, 
            roleCode = roleCode,
            userId = res.userId,
            email = res.email,
            fullName = res.fullName
        )
        
        tokenStore.save(auth)
        ApiClient.setToken(res.access_token)
        return auth
    }

    suspend fun login(email: String, password: String): Authenticated {
        return login(LoginReq(email.trim(), password))
    }

    suspend fun logout() {
        tokenStore.clear()
        ApiClient.setToken(null)
    }
}
