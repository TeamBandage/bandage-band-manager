package com.bandage.v1.global.async.publisher

import com.bandage.v1.global.async.event.CommonEvent

interface EventPublisher {
    fun publish(event: CommonEvent)
}
