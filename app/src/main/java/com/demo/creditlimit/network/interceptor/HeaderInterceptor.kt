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
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
            .header("App-Version", appVersion)
            .header("Platform", "Android")
            .header("Device-Model", deviceModel)
        // Only set default Content-Type when not already specified (e.g. octet-stream uploads)
        if (original.header("Content-Type") == null) {
            builder.header("Content-Type", "application/json")
        }
        return chain.proceed(builder.build())
    }
}
