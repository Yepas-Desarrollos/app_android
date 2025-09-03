package mx.checklist.data.api

// Importaciones explícitas para DTOs usados en las firmas de los métodos
import mx.checklist.data.api.dto.LoginReq
import mx.checklist.data.api.dto.LoginRes
import mx.checklist.data.api.dto.StoreDto
import mx.checklist.data.api.dto.TemplateDto
import mx.checklist.data.api.dto.CreateRunReq
import mx.checklist.data.api.dto.RunRes
import mx.checklist.data.api.dto.RunItemDto
import mx.checklist.data.api.dto.RespondReq

import retrofit2.http.*

interface Api {
    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): LoginRes

    @GET("stores")
    @Authenticated
    suspend fun getStores(): List<StoreDto>

    @GET("templates")
    @Authenticated
    suspend fun getTemplates(): List<TemplateDto>

    @POST("runs")
    @Authenticated
    suspend fun createRun(@Body body: CreateRunReq): RunRes

    @GET("runs/{id}/items")
    @Authenticated
    suspend fun getRunItems(@Path("id") runId: Long): List<RunItemDto>

    @POST("items/{id}/respond")
    @Authenticated
    suspend fun respondItem(@Path("id") itemId: Long, @Body body: RespondReq): RunItemDto

    @POST("runs/{id}/submit")
    @Authenticated
    suspend fun submitRun(@Path("id") runId: Long): RunRes
}
