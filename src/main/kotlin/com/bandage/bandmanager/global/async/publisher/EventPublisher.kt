package com.bandage.bandmanager.global.async.publisher

import com.bandage.bandmanager.global.async.event.CommonEvent

interface EventPublisher {
    fun publish(event: CommonEvent)
}
