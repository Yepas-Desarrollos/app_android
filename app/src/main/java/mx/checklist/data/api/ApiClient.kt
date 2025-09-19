package mx.checklist.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import mx.checklist.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    private val authInterceptor = Interceptor { chain ->
        val t = token
        val req = if (t.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $t")
                .build()
        }
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Cliente regular para operaciones normales
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Cliente específico para uploads con timeouts más largos
    private val uploadClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(120, TimeUnit.SECONDS)    // 2 minutos para conectar
        .readTimeout(300, TimeUnit.SECONDS)       // 5 minutos para leer respuesta
        .writeTimeout(300, TimeUnit.SECONDS)      // 5 minutos para escribir datos
        .retryOnConnectionFailure(true)           // Reintentar en caso de fallos de conexión
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL) // p.ej. http://172.16.16.22:3000/
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()

    // Retrofit específico para uploads
    private val uploadRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(uploadClient)
        .build()

    val api: Api = retrofit.create(Api::class.java)
    val uploadApi: Api = uploadRetrofit.create(Api::class.java)
}