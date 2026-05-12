package com.demo.creditlimit.network.exception

/**
 * Maps server/HTTP error codes to user-facing messages.
 * Keeps UI layer free of raw numeric codes.
 */
object ErrorMessageMapper {

    fun getMessage(code: Int): String = when (code) {
        ErrorCode.UNAUTHORIZED -> "Unauthorized. Please log in again."
        ErrorCode.FORBIDDEN -> "You do not have permission to perform this action."
        ErrorCode.NOT_FOUND -> "The requested resource was not found."
        ErrorCode.SERVER_ERROR -> "Server error. Please try again later."
        ErrorCode.TOKEN_EXPIRED -> "Your session has expired. Please log in again."
        ErrorCode.ACCOUNT_DISABLED -> "Your account has been disabled. Please contact support."
        ErrorCode.INSUFFICIENT_PERMISSION -> "Insufficient permission for this operation."
        ErrorCode.KYC_ALREADY_SUBMITTED -> "KYC information has already been submitted."
        ErrorCode.CREDIT_EVALUATION_PENDING -> "Credit evaluation is still in progress."
        else -> "An error occurred (code: $code). Please try again."
    }

    /** Convert a [NetworkException] to a user-facing string. */
    fun toUserMessage(exception: NetworkException): String = when (exception) {
        is NetworkException.NetworkError -> exception.message
        is NetworkException.TimeoutError -> exception.message
        is NetworkException.TokenExpired -> exception.message
        is NetworkException.Unauthorized -> exception.message
        is NetworkException.Forbidden -> exception.message
        is NetworkException.NotFound -> exception.message
        is NetworkException.ServerError -> exception.message
        is NetworkException.ParseError -> "Unexpected response from server. Please try again."
        is NetworkException.ApiError -> exception.message.ifBlank { getMessage(exception.code) }
        is NetworkException.UnknownError -> exception.message
    }
}
