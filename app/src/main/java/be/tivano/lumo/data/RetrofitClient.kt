package be.tivano.lumo.data

import android.content.Context
import be.tivano.lumo.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL = BuildConfig.BASE_URL

    @Volatile
    private var apiServiceInstance: ApiService? = null

    private lateinit var tokenManager: TokenManager

    fun initialize(context: Context) {
        if (apiServiceInstance == null) {
            synchronized(this) {
                if (apiServiceInstance == null) {
                    tokenManager = TokenManager(context.applicationContext)
                    apiServiceInstance = createApiService()
                }
            }
        }
    }

    private fun createApiService(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    val apiService: ApiService
        get() = apiServiceInstance ?: throw IllegalStateException(
            "RetrofitClient not initialized. Call RetrofitClient.initialize(context) first."
        )
}
