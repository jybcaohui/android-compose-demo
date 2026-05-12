package com.demo.creditlimit.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Retries requests that fail with an [IOException] (network-level errors).
 *
 * Only GET requests are retried automatically to avoid duplicate submissions
 * on non-idempotent endpoints.  POST/PUT callers can opt in by adding
 * the header "Retry: true".
 *
 * Back-off: attempt N waits N * [backoffMs] ms before the next try.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val backoffMs: Long = 1_000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isRetryable = request.method == "GET" || request.header("Retry") == "true"

        if (!isRetryable) return chain.proceed(request)

        var lastException: IOException? = null

        repeat(maxRetries) { attempt ->
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) return response
                // Close non-successful response before retrying
                response.close()
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < maxRetries - 1) {
                Thread.sleep(backoffMs * (attempt + 1))
            }
        }

        throw lastException ?: IOException("Request failed after $maxRetries attempts.")
    }
}
