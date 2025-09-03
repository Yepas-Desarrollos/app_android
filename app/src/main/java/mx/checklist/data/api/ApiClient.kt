package mx.checklist.data.api

import mx.checklist.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile private var token: String? = null
    fun setToken(newToken: String?) { token = newToken }

    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: Api by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // ← Debe terminar con "/" (p.ej. http://192.168.1.50:3000/)
            .addConverterFactory(MoshiConverterFactory.create(moshi)) // ← CLAVE para el @Body converter
            .client(okHttp)
            .build()
            .create(Api::class.java)
    }
}
