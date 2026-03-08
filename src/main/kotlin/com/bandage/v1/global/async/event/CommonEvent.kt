package com.bandage.v1.global.async.event

import com.github.f4b6a3.uuid.UuidCreator
import java.time.LocalDateTime
import java.util.UUID

abstract class CommonEvent(
    val eventId: UUID = UuidCreator.getTimeOrderedEpoch(),
    val eventType: EventType,
    val aggregateId: String,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
