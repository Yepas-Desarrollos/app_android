package mx.checklist.data.api

import mx.checklist.data.api.dto.*
import retrofit2.http.*

interface ChecklistApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): LoginRes

    @GET("stores")
    suspend fun getStores(): List<StoreDto>

    @GET("templates")
    suspend fun getTemplates(): List<TemplateDto>

    @POST("runs")
    suspend fun createRun(@Body body: CreateRunReq): RunRes

    @GET("runs/{id}/items")
    suspend fun getRunItems(@Path("id") runId: Long): List<RunItemDto>

    @POST("items/{id}/respond")
    suspend fun respondItem(@Path("id") itemId: Long, @Body body: RespondReq): RunItemDto

    @PATCH("runs/{id}/submit")
    suspend fun submitRun(@Path("id") runId: Long): RunRes
}
