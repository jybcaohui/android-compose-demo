package com.demo.creditlimit.network.state

import com.demo.creditlimit.network.exception.NetworkException

/**
 * Unified wrapper for all network operation states.
 * Empty = request succeeded but response body carried no data (void endpoints).
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: NetworkException) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
    data object Empty : NetworkResult<Nothing>()
}
