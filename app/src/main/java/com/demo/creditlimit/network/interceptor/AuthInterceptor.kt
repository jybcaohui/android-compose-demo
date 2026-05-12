package com.demo.creditlimit.network.interceptor

import com.demo.creditlimit.network.manager.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects the Bearer token from [TokenManager]'s in-memory cache.
 * Uses the synchronous cache so the interceptor stays non-blocking.
 *
 * Routes annotated with @Headers("No-Auth: true") are skipped
 * (e.g. login, register).
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth for public endpoints
        if (originalRequest.header("No-Auth") == "true") {
            return chain.proceed(
                originalRequest.newBuilder().removeHeader("No-Auth").build()
            )
        }

        val token = tokenManager.getCachedToken()
        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("x-app-token", token)
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
