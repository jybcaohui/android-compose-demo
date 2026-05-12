package com.demo.creditlimit.network.exception

object ErrorCode {
    const val SUCCESS = 200
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val SERVER_ERROR = 500

    // Business-specific codes
    const val TOKEN_EXPIRED = 701
    const val ACCOUNT_DISABLED = 702
    const val INSUFFICIENT_PERMISSION = 703
    const val KYC_ALREADY_SUBMITTED = 704
    const val CREDIT_EVALUATION_PENDING = 705
}
