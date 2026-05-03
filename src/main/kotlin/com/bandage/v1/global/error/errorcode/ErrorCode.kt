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

    // setlist meeting
    SETLIST_MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 선곡 회의 정보를 찾을 수 없습니다."),
    SETLIST_MEETING_FORBIDDEN(HttpStatus.FORBIDDEN, "선곡 회의에 접근할 권한이 없습니다."),
    SETLIST_MEETING_NOT_MANAGER(HttpStatus.FORBIDDEN, "선곡 회의 매니저만 수행할 수 있는 작업입니다."),
    SETLIST_MEETING_LOCKED(HttpStatus.CONFLICT, "잠금 상태의 선곡 회의입니다."),
    SETLIST_MEETING_NOT_LOCKED(HttpStatus.CONFLICT, "잠금 상태가 아닌 선곡 회의입니다."),
    SETLIST_MANAGER_NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "매니저는 참여 멤버에 포함되어야 합니다."),
    SETLIST_PERFORMANCE_REQUIRED(HttpStatus.BAD_REQUEST, "공연 모드 회의에는 performanceId가 필요합니다."),
    SETLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 선곡 항목 정보를 찾을 수 없습니다."),
    SETLIST_ITEM_FORBIDDEN(HttpStatus.FORBIDDEN, "선곡 항목을 수정할 권한이 없습니다."),
    SETLIST_ITEM_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 선곡 항목 세션을 찾을 수 없습니다."),
    SETLIST_ITEM_SESSION_FULL(HttpStatus.BAD_REQUEST, "세션 정원을 초과했습니다."),
    SETLIST_CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "채팅 메시지는 500자를 초과할 수 없습니다."),
    SETLIST_PERFORMANCE_HAS_ACTIVE_MEETING(HttpStatus.CONFLICT, "해당 공연에는 이미 활성 선곡 회의가 존재합니다."),
    SETLIST_CANNOT_REMOVE_MANAGER(HttpStatus.BAD_REQUEST, "매니저는 회의 참여자에서 제거할 수 없습니다."),
    SETLIST_PRACTICE_WINDOW_REQUIRED(HttpStatus.BAD_REQUEST, "purpose=GENERAL 인 회의는 practiceWindow 가 필수입니다."),
    SETLIST_PRACTICE_WINDOW_INVALID(HttpStatus.BAD_REQUEST, "practiceWindow.from 은 to 보다 같거나 이전이어야 합니다."),
    SETLIST_NOT_LOCKED(HttpStatus.BAD_REQUEST, "선곡 회의가 lock 상태가 아닙니다. 시안 확정 전 회의를 lock 해주세요."),

    // schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 스케줄 정보를 찾을 수 없습니다."),
    SCHEDULE_BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 시간표 시안을 찾을 수 없습니다."),
    SCHEDULE_BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 스케줄 블록을 찾을 수 없습니다."),
    SCHEDULE_BOARD_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "회의당 시간표 시안은 최대 5개까지 생성할 수 있습니다."),
    SCHEDULE_BOARD_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 시안이 존재합니다. 기존 시안을 unconfirm 후 진행하세요."),
    SCHEDULE_BOARD_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "확정되지 않은 시안입니다."),
    SCHEDULE_DATES_OVERLAP(HttpStatus.BAD_REQUEST, "availableDates와 unavailableDates에 중복된 날짜가 있습니다."),
    SCHEDULE_DATE_OUT_OF_WINDOW(HttpStatus.BAD_REQUEST, "선택한 날짜가 practiceWindow 범위를 벗어났습니다."),
    SCHEDULE_SLOT_INVALID(HttpStatus.BAD_REQUEST, "startSlot + durationSlots는 48을 초과할 수 없습니다."),
}
