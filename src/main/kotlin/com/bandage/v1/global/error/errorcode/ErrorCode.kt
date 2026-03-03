package com.bandage.v1.global.error.errorcode

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "올바르지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 사용자 정보를 찾을 수 없습니다."),

    // band
    BAND_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 밴드 정보를 찾을 수 없습니다."),
    DUPLICATE_BAND_NAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 밴드 이름입니다."),

    // practice
    PRACTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 합주 정보를 찾을 수 없습니다."),

    // performance
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 공연 정보를 찾을 수 없습니다."),
}
