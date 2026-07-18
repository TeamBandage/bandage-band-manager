package com.bandage.bandmanager.domain.schedule.dto.res

import java.time.LocalDateTime

data class ScheduleUnconfirmResponse(
    val unconfirmedAt: LocalDateTime,
)
