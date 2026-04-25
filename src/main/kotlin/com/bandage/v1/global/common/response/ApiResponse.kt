package com.bandage.v1.global.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val code: String? = null,
    val data: T? = null,
    val fieldErrors: Map<String, String>? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun <T> success(data: T? = null): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
            )

        fun error(
            message: String? = null,
            code: String? = null,
            fieldErrors: Map<String, String>? = null,
        ): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                message = message,
                code = code,
                fieldErrors = fieldErrors,
            )
    }
}
