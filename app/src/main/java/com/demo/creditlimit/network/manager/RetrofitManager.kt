package com.demo.creditlimit.network.manager

import android.content.Context
import com.demo.creditlimit.network.api.ApiService
import com.demo.creditlimit.network.interceptor.AuthInterceptor
import com.demo.creditlimit.network.interceptor.HeaderInterceptor
import com.demo.creditlimit.network.interceptor.LoggingInterceptor
import com.demo.creditlimit.network.interceptor.RetryInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit factory.
 *
 * Lifecycle:
 *   1. Call [init] once in Application.onCreate().
 *   2. Obtain the API interface via [getApiService].
 *   3. After an environment switch, call [rebuild] to recreate the client
 *      with the new base URL.
 */
object RetrofitManager {

    private const val CONNECT_TIMEOUT_SEC = 30L
    private const val READ_TIMEOUT_SEC = 30L
    private const val WRITE_TIMEOUT_SEC = 30L

    @Volatile
    private var apiService: ApiService? = null

    fun init(context: Context) {
        apiService = buildApiService(context)
    }

    fun rebuild(context: Context) {
        apiService = null
        init(context)
    }

    fun getApiService(): ApiService =
        apiService ?: error("RetrofitManager not initialized — call init() in Application.onCreate().")

    private fun buildApiService(context: Context): ApiService {
        val tokenManager = TokenManager.getInstance(context)

        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")

        val gson = GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            // Order matters: Header → Auth → Retry → Logging (outermost = first logged)
            .addInterceptor(HeaderInterceptor(appVersion = appVersion))
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(LoggingInterceptor(enabled = EnvironmentManager.isDebug()))
            .build()

        return Retrofit.Builder()
            .baseUrl(EnvironmentManager.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
