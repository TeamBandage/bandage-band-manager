package com.bandage.v1.global.common.response

import java.time.LocalDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun <T> success(data: T? = null): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
            )

        fun error(message: String? = null): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                message = message,
            )
    }
}
