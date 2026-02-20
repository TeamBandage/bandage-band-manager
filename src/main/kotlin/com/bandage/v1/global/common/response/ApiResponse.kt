package com.bandage.v1.global.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
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
