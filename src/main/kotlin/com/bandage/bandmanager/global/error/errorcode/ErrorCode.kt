package com.bandage.bandmanager.global.error.errorcode

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
    OAUTH_LOCAL_LOGIN_NOT_ALLOWED(HttpStatus.CONFLICT, "소셜 로그인으로 가입된 계정입니다. 해당 소셜 로그인을 이용해주세요."),
    OAUTH_PASSWORD_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT, "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다."),
    OAUTH_PROVIDER_MISMATCH(HttpStatus.CONFLICT, "다른 소셜 계정으로 가입된 이메일입니다."),
    OAUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 소셜 인증 토큰입니다."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "소셜 인증 서버와의 통신에 실패했습니다."),

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

    // jam
    JAM_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 합주 정보를 찾을 수 없습니다."),
    JAM_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 합주 세션 정보를 찾을 수 없습니다."),
    JAM_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "합주 참여자 정보를 찾을 수 없습니다."),
    JAM_PARTICIPANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 합주에 참여 중인 멤버입니다."),

    // performance
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 공연 정보를 찾을 수 없습니다."),
    NOT_A_PERFORMANCE_MANAGER(HttpStatus.FORBIDDEN, "공연 매니저만 수행할 수 있는 작업입니다."),
    NOT_A_PERFORMANCE_OWNER(HttpStatus.FORBIDDEN, "공연 소유자(OWNER)만 수행할 수 있는 작업입니다."),
    PERFORMANCE_SETLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "공연에 연결된 셋리스트 정보를 찾을 수 없습니다."),
    ALREADY_PERFORMANCE_MANAGER(HttpStatus.CONFLICT, "이미 공연에 참여 중인 멤버입니다."),
    PERFORMANCE_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 공연 초대 정보를 찾을 수 없습니다."),
    PERFORMANCE_INVITATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 발송되어 대기 중인 초대입니다."),
    PERFORMANCE_INVITATION_FORBIDDEN(HttpStatus.FORBIDDEN, "공연 초대를 처리할 권한이 없습니다."),

    // setlist meeting
    SETLIST_MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 선곡 회의 정보를 찾을 수 없습니다."),
    SETLIST_MEETING_FORBIDDEN(HttpStatus.FORBIDDEN, "선곡 회의에 접근할 권한이 없습니다."),
    SETLIST_MEETING_NOT_MANAGER(HttpStatus.FORBIDDEN, "선곡 회의 매니저만 수행할 수 있는 작업입니다."),
    SETLIST_MEETING_LOCKED(HttpStatus.CONFLICT, "잠금 상태의 선곡 회의입니다."),
    SETLIST_MEETING_NOT_LOCKED(HttpStatus.CONFLICT, "잠금 상태가 아닌 선곡 회의입니다."),
    SETLIST_MANAGER_NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "매니저는 참여 멤버에 포함되어야 합니다."),
    SETLIST_PERFORMANCE_REQUIRED(HttpStatus.BAD_REQUEST, "공연 모드 회의에는 performanceId가 필요합니다."),
    SETLIST_MEETING_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 선곡 회의 항목 정보를 찾을 수 없습니다."),
    SETLIST_MEETING_ITEM_FORBIDDEN(HttpStatus.FORBIDDEN, "선곡 회의 항목을 수정할 권한이 없습니다."),
    SETLIST_MEETING_ITEM_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 선곡 회의 항목 세션을 찾을 수 없습니다."),
    SETLIST_MEETING_ITEM_SESSION_FULL(HttpStatus.BAD_REQUEST, "세션 정원을 초과했습니다."),
    SETLIST_CHAT_MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "채팅 메시지는 500자를 초과할 수 없습니다."),
    SETLIST_PERFORMANCE_HAS_ACTIVE_MEETING(HttpStatus.CONFLICT, "해당 공연에는 이미 활성 선곡 회의가 존재합니다."),
    SETLIST_CANNOT_REMOVE_MANAGER(HttpStatus.BAD_REQUEST, "매니저는 회의 참여자에서 제거할 수 없습니다."),
    SETLIST_PRACTICE_WINDOW_REQUIRED(HttpStatus.BAD_REQUEST, "purpose=GENERAL 인 회의는 practiceWindow 가 필수입니다."),
    SETLIST_PRACTICE_WINDOW_INVALID(HttpStatus.BAD_REQUEST, "practiceWindow.from 은 to 보다 같거나 이전이어야 합니다."),
    SETLIST_NOT_LOCKED(HttpStatus.BAD_REQUEST, "선곡 회의가 lock 상태가 아닙니다. 시안 확정 전 회의를 lock 해주세요."),
    SETLIST_SELECTION_INCOMPLETE_SESSION(HttpStatus.BAD_REQUEST, "모든 세션의 확정 인원이 충족되지 않았습니다."),
    SETLIST_NO_SELECTED_TRACK(HttpStatus.BAD_REQUEST, "선택된 트랙이 없습니다. 최소 1개 이상의 트랙을 선택해주세요."),
    SETLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 셋리스트 정보를 찾을 수 없습니다."),
    SETLIST_FORBIDDEN(HttpStatus.FORBIDDEN, "셋리스트에 접근할 권한이 없습니다."),
    SETLIST_NOT_MANAGER(HttpStatus.FORBIDDEN, "셋리스트 매니저만 수행할 수 있는 작업입니다."),
    SETLIST_TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 셋리스트 트랙 정보를 찾을 수 없습니다."),

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
    SCHEDULE_BOARD_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 시안을 먼저 수정했습니다. 새로고침 후 다시 시도해주세요."),

    // availability
    AVAILABILITY_INVALID(HttpStatus.BAD_REQUEST, "가용성 정보가 올바르지 않습니다."),

    // schedule (performance scope)
    SCHEDULE_PERFORMANCE_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 공연 일정에 접근할 권한이 없습니다."),
    SCHEDULE_BLOCK_TRACK_REQUIRED(HttpStatus.BAD_REQUEST, "블록에는 최소 1개 이상의 트랙이 필요합니다."),
    SCHEDULE_BLOCK_TRACK_NOT_IN_PERFORMANCE(HttpStatus.BAD_REQUEST, "공연의 셋리스트에 속하지 않은 트랙입니다."),
    SCHEDULE_WINDOW_REQUIRED(HttpStatus.BAD_REQUEST, "자동 배치를 위해서는 연습 가능 기간(window)이 필요합니다."),
    SCHEDULE_NO_PLACEABLE_TRACK(HttpStatus.BAD_REQUEST, "배치할 트랙이 없습니다. 공연 셋리스트를 확인하세요."),

    // upload
    INVALID_FILE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 타입입니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 확장자입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "허용된 파일 크기를 초과했습니다."),

    // notify
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 알림 정보를 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "알림에 접근할 권한이 없습니다."),
}
