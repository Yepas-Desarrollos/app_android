package mx.checklist.data.api.dto

import com.squareup.moshi.Json

data class LoginReq(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

data class LoginRes(
    @Json(name = "access_token") val accessToken: String
)
