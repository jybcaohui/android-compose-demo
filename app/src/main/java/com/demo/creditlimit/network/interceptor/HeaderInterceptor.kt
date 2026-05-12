package com.demo.creditlimit.network.interceptor

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches static headers to every request:
 * Content-Type, Accept, App-Version, Platform, Device-Model.
 */
class HeaderInterceptor(
    private val appVersion: String,
    private val deviceModel: String = Build.MODEL
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("App-Version", appVersion)
            .header("Platform", "Android")
            .header("Device-Model", deviceModel)
            .build()
        return chain.proceed(request)
    }
}
