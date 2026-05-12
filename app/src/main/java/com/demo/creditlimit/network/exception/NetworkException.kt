package com.demo.creditlimit.network.exception

/**
 * Sealed hierarchy for all network errors.
 * Each subtype carries a stable [code] so callers can switch on it
 * without string-matching on [message].
 */
sealed class NetworkException(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /** Server returned a non-2xx HTTP status or a business error code. */
    class ApiError(code: Int, message: String) : NetworkException(code, message)

    /** 401 — token missing or invalid. */
    class Unauthorized(message: String = "Unauthorized. Please log in again.") :
        NetworkException(401, message)

    /** 403 — authenticated but no permission. */
    class Forbidden(message: String = "You do not have permission to perform this action.") :
        NetworkException(403, message)

    /** 404 */
    class NotFound(message: String = "The requested resource was not found.") :
        NetworkException(404, message)

    /** 5xx */
    class ServerError(message: String = "Server error. Please try again later.") :
        NetworkException(500, message)

    /** 701 — custom business code: session expired. */
    class TokenExpired(message: String = "Your session has expired. Please log in again.") :
        NetworkException(701, message)

    /** No internet / DNS failure. */
    class NetworkError(
        message: String = "No internet connection. Please check your network.",
        cause: Throwable? = null
    ) : NetworkException(-1, message, cause)

    /** Socket timeout. */
    class TimeoutError(message: String = "Request timed out. Please try again.") :
        NetworkException(-2, message)

    /** Gson / JSON parse failure. */
    class ParseError(
        message: String = "Failed to parse server response.",
        cause: Throwable? = null
    ) : NetworkException(-3, message, cause)

    /** Catch-all. */
    class UnknownError(
        message: String = "An unexpected error occurred.",
        cause: Throwable? = null
    ) : NetworkException(-99, message, cause)
}
