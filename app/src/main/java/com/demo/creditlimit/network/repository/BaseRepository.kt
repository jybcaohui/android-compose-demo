package com.demo.creditlimit.network.repository

import com.demo.creditlimit.network.response.BaseResponse
import com.demo.creditlimit.network.state.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Provides [executeRequest] which wraps any suspend API call in a
 * Flow that emits [NetworkResult.Loading] first, then the result.
 *
 * All concrete repositories extend this class.
 */
abstract class BaseRepository {

    protected fun <T> executeRequest(
        apiCall: suspend () -> BaseResponse<T>
    ): Flow<NetworkResult<T>> = flow {
        emit(NetworkResult.Loading)
        emit(safeApiCall { apiCall() })
    }
}
