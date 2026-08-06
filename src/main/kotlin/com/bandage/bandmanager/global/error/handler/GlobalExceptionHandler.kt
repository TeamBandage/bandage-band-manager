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
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
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

    /**
     * 경로변수·쿼리 파라미터의 타입 변환 실패(예: UUID 자리에 숫자, 잘못된 날짜 형식).
     * 이 핸들러가 없으면 아래 catch-all 로 떨어져 클라이언트 입력 오류가 500 으로 응답된다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    protected fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ErrorCode.INVALID_INPUT_VALUE
        return ResponseEntity
            .status(errorCode.status)
            .body(
                ApiResponse.error(
                    message = errorCode.message,
                    code = errorCode.name,
                    fieldErrors = mapOf(e.name to "올바르지 않은 입력 형식입니다."),
                ),
            )
    }

    /**
     * 필수 쿠키 누락(예: refresh 토큰 재발급 시 refreshToken 쿠키 없음).
     * 인증 실패이므로 401 로 응답해 클라이언트가 재로그인 분기를 태울 수 있게 한다.
     */
    @ExceptionHandler(MissingRequestCookieException::class)
    protected fun handleMissingRequestCookie(e: MissingRequestCookieException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ErrorCode.UNAUTHORIZED
        return ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse.error(message = errorCode.message, code = errorCode.name))
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

    /**
     * 위 handleException 은 커스텀 Exception 타입만 잡으므로, 그 외 표준 예외
     * (예: DataIntegrityViolationException)는 여기서 500 으로 매핑한다.
     * 이 폴백이 없으면 미처리 예외가 Security 필터 체인까지 전파되어 401 로 오응답된다.
     */
    @ExceptionHandler(java.lang.Exception::class)
    protected fun handleUnexpectedException(e: java.lang.Exception): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR
        val response = ApiResponse.error(message = errorCode.message, code = errorCode.name)
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
