package com.bandage.bandmanager.global.error.handler

import com.bandage.bandmanager.global.common.response.ApiResponse
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.error.exception.Exception
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.KotlinInvalidNullException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
open class GlobalExceptionHandler {
    @ExceptionHandler
    protected fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = e.errorCode
        return ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse.error(message = errorCode.message, code = errorCode.name))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val fieldErrors =
            e.bindingResult.fieldErrors
                .associate { it.field to (it.defaultMessage ?: "잘못된 입력값입니다.") }
        val firstMessage = fieldErrors.values.firstOrNull() ?: ErrorCode.INVALID_INPUT_VALUE.message
        val response =
            ApiResponse.error(
                message = firstMessage,
                code = ErrorCode.INVALID_INPUT_VALUE.name,
                fieldErrors = fieldErrors.takeIf { it.isNotEmpty() },
            )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    protected fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        val (message, fieldErrors) =
            when (val cause = e.cause) {
                is KotlinInvalidNullException -> {
                    val field = cause.kotlinPropertyName.takeIf { it != "UNKNOWN" } ?: cause.path.firstFieldName()
                    val msg = "필수 필드가 누락되었습니다."
                    msg to (field?.let { mapOf(it to msg) })
                }
                is InvalidFormatException -> {
                    val field = cause.path.firstFieldName()
                    val msg = "올바르지 않은 입력 형식입니다."
                    msg to (field?.let { mapOf(it to msg) })
                }
                is MismatchedInputException -> {
                    val field = cause.path.firstFieldName()
                    val msg = ErrorCode.INVALID_INPUT_VALUE.message
                    msg to (field?.let { mapOf(it to msg) })
                }
                else -> ErrorCode.INVALID_INPUT_VALUE.message to null
            }
        val response =
            ApiResponse.error(
                message = message,
                code = ErrorCode.INVALID_INPUT_VALUE.name,
                fieldErrors = fieldErrors,
            )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    private fun List<com.fasterxml.jackson.databind.JsonMappingException.Reference>.firstFieldName(): String? = firstOrNull()?.fieldName

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    protected fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ErrorCode.METHOD_NOT_ALLOWED
        return ResponseEntity
            .status(errorCode.status)
            .body(
                ApiResponse.error(
                    message = "${errorCode.message} (요청 메서드: ${e.method})",
                    code = errorCode.name,
                ),
            )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    protected fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ErrorCode.RESOURCE_NOT_FOUND
        return ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse.error(message = errorCode.message, code = errorCode.name))
    }

    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR
        val response = ApiResponse.error(message = errorCode.message, code = errorCode.name)
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
