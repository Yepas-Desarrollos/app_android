package mx.checklist.data.api

import mx.checklist.data.api.dto.CreateRunReq
import mx.checklist.data.api.dto.LoginReq
import mx.checklist.data.api.dto.RespondReq
import mx.checklist.data.api.dto.RunItemDto
import mx.checklist.data.api.dto.RunRes
import mx.checklist.data.api.dto.StoreDto
import mx.checklist.data.api.dto.TemplateDto
import mx.checklist.data.api.dto.TokenRes
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface Api {
    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): TokenRes

    @GET("stores")
    suspend fun stores(): List<StoreDto>

    @GET("templates")
    suspend fun templates(): List<TemplateDto>

    @POST("runs")
    suspend fun createRun(@Body body: CreateRunReq): RunRes

    @GET("runs/{id}/items")
    suspend fun runItems(@Path("id") runId: Long): List<RunItemDto>

    @POST("items/{id}/respond")
    suspend fun respond(
        @Path("id") itemId: Long,
        @Body body: RespondReq
    ): RunItemDto

    @PATCH("runs/{id}/submit")
    suspend fun submit(@Path("id") runId: Long): RunRes
}
