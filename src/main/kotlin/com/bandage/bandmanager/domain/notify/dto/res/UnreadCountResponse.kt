package com.bandage.bandmanager.domain.notify.dto.res

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "미확인 알림 개수 응답")
data class UnreadCountResponse(
    @Schema(description = "미확인(읽지 않은) 알림 개수", example = "3")
    val count: Long,
)
