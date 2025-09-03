package mx.checklist.data.net

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import mx.checklist.BuildConfig
import mx.checklist.data.api.ChecklistApi
import mx.checklist.data.auth.AuthState
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private val authInterceptor = Interceptor { chain ->
    val original = chain.request()
    val builder = original.newBuilder()
    AuthState.token?.let { t ->
        builder.addHeader("Authorization", "Bearer $t")
    }
    chain.proceed(builder.build())
}

object RetrofitClient {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ChecklistApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // Debe terminar en "/"
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()
            .create(ChecklistApi::class.java)
    }
}
