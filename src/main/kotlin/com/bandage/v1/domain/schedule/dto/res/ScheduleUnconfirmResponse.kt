package com.bandage.v1.domain.schedule.dto.res

import java.time.LocalDateTime

data class ScheduleUnconfirmResponse(
    val unconfirmedAt: LocalDateTime,
)
