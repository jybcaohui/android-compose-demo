package com.demo.creditlimit.network.response

import com.google.gson.annotations.SerializedName

/**
 * Unified API envelope.
 *
 * Every endpoint returns:
 * { "code": 200, "message": "success", "data": { ... } }
 *
 * [data] is nullable to handle void endpoints gracefully.
 */
data class BaseResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String,
    @SerializedName("data") val data: T?
) {
    fun isSuccess(): Boolean = code == 200
}
