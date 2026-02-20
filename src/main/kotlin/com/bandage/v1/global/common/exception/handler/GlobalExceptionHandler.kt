package com.bandage.v1.global.common.exception.handler

import com.bandage.v1.global.common.exception.errorcode.ErrorCode
import com.bandage.v1.global.common.exception.exception.BusinessException
import com.bandage.v1.global.common.exception.exception.Exception
import com.bandage.v1.global.common.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.bandage.v1.domain"])
open class GlobalExceptionHandler {
    @ExceptionHandler
    protected fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = e.errorCode
        return ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse.error(errorCode.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors[0].defaultMessage ?: "잘못된 요청입니다."
        val response = ApiResponse.error(message)
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        val response = ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.message)
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
