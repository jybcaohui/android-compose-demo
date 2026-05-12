package com.demo.creditlimit.network.repository

import com.demo.creditlimit.network.exception.ErrorCode
import com.demo.creditlimit.network.exception.ErrorMessageMapper
import com.demo.creditlimit.network.exception.NetworkException
import com.demo.creditlimit.network.response.BaseResponse
import com.demo.creditlimit.network.state.NetworkResult
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unified suspend wrapper for every API call.
 *
 * Maps server envelopes and all common exceptions → [NetworkResult]:
 * - HTTP 2xx + business code 200 → [NetworkResult.Success] (or [NetworkResult.Empty] when data is null)
 * - Business error codes → specific [NetworkException] subtypes
 * - [HttpException], [IOException], [JsonSyntaxException] → mapped exceptions
 *
 * Usage inside a repository:
 * ```
 * suspend fun login(...) = safeApiCall { apiService.login(request) }
 * ```
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> BaseResponse<T>
): NetworkResult<T> = withContext(Dispatchers.IO) {
    try {
        val response = apiCall()

        when {
            !response.isSuccess() -> mapBusinessError(response.code, response.msg)

            response.data != null -> NetworkResult.Success(response.data)

            else -> NetworkResult.Empty
        }
    } catch (e: HttpException) {
        val msg = ErrorMessageMapper.getMessage(e.code())
        NetworkResult.Error(NetworkException.ApiError(e.code(), msg))
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error(NetworkException.TimeoutError())
    } catch (e: UnknownHostException) {
        NetworkResult.Error(NetworkException.NetworkError(cause = e))
    } catch (e: IOException) {
        NetworkResult.Error(NetworkException.NetworkError(e.message ?: "Network error", e))
    } catch (e: JsonSyntaxException) {
        NetworkResult.Error(NetworkException.ParseError(cause = e))
    } catch (e: Exception) {
        NetworkResult.Error(NetworkException.UnknownError(e.message ?: "Unknown error", e))
    }
}

private fun <T> mapBusinessError(code: Int, serverMessage: String): NetworkResult<T> {
    val message = serverMessage.ifBlank { ErrorMessageMapper.getMessage(code) }
    val exception = when (code) {
        ErrorCode.TOKEN_EXPIRED -> NetworkException.TokenExpired(message)
        ErrorCode.UNAUTHORIZED -> NetworkException.Unauthorized(message)
        ErrorCode.FORBIDDEN -> NetworkException.Forbidden(message)
        ErrorCode.NOT_FOUND -> NetworkException.NotFound(message)
        ErrorCode.SERVER_ERROR -> NetworkException.ServerError(message)
        else -> NetworkException.ApiError(code, message)
    }
    return NetworkResult.Error(exception)
}
