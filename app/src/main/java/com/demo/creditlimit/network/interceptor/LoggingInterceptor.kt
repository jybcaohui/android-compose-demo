package com.demo.creditlimit.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Wraps OkHttp's [HttpLoggingInterceptor] and routes output to Logcat
 * under the "NetworkLog" tag.  Only active when [enabled] is true
 * (should be false in production builds).
 */
class LoggingInterceptor(private val enabled: Boolean = true) : Interceptor {

    private val delegate = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    override fun intercept(chain: Interceptor.Chain): Response =
        if (enabled) delegate.intercept(chain) else chain.proceed(chain.request())

    companion object {
        private const val TAG = "NetworkLog"
    }
}
