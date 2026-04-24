package com.bandage.v1.global.error.errorcode

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "올바르지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    NO_CHANGE(HttpStatus.BAD_REQUEST, "변경된 사항이 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 회원 정보를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 회원입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호입니다."),
    DUPLICATE_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호를 사용할 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 회원가입된 e-mail 입니다"),

    // auth
    MEMBER_AUTH_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 인증 정보를 찾을 수 없습니다."),
    MEMBER_AUTH_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 회원가입된 e-mail 입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "만료된 토큰입니다."),

    // band
    BAND_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 밴드 정보를 찾을 수 없습니다."),
    BAND_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 밴드 가입 신청 정보를 찾을 수 없습니다."),
    BAND_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 밴드 멤버 정보를 찾을 수 없습니다."),
    DUPLICATE_BAND_NAME(HttpStatus.CONFLICT, "이미 사용 중인 밴드 이름입니다."),
    NOT_A_LEADER(HttpStatus.FORBIDDEN, "리소스를 조회 또는 처리할 권한이 없습니다."),
    BAND_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 밴드 멤버입니다."),
    DUPLICATE_BAND_APPLICATION(HttpStatus.CONFLICT, "이미 밴드 가입이 처리 중이거나 처리가 완료되었습니다."),
    UNABLE_TO_WITHDRAW(HttpStatus.BAD_REQUEST, "가입 신청이 존재하지 않거나, 기 신청을 취소할 수 없는 상태입니다."),
    BAND_APPLICATION_NOT_BELONGS_TO_BAND(HttpStatus.BAD_REQUEST, "해당 밴드에 대한 가입 신청이 아닙니다"),
    ABNORMAL_LEADER_COUNT(HttpStatus.INTERNAL_SERVER_ERROR, "밴드 리더 양도 처리 중 오류가 발생했습니다"),
    LEADER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "해당 밴드에 대한 가입 신청이 아닙니다"),

    // practice
    PRACTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 합주 정보를 찾을 수 없습니다."),
    PRACTICE_SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 합주 곡 정보를 찾을 수 없습니다."),
    PRACTICE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 합주 세션 정보를 찾을 수 없습니다."),
    PRACTICE_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "합주 참여자 정보를 찾을 수 없습니다."),
    PRACTICE_PARTICIPANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 합주에 참여 중인 멤버입니다."),
    PRACTICE_SESSION_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "이미 배정된 세션입니다."),

    // performance
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 공연 정보를 찾을 수 없습니다."),
    NOT_A_PERFORMANCE_MANAGER(HttpStatus.FORBIDDEN, "공연 매니저만 수행할 수 있는 작업입니다."),
    PERFORMANCE_PRACTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공연에 등록된 합주 정보를 찾을 수 없습니다."),
}
