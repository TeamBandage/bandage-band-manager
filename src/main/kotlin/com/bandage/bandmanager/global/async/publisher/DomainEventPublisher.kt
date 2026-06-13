package com.bandage.bandmanager.global.async.publisher

import com.bandage.bandmanager.global.async.event.CommonEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class DomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : EventPublisher {
    override fun publish(event: CommonEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
