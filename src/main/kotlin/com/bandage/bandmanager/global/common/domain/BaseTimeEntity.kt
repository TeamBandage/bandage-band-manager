package com.bandage.bandmanager.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
open class BaseTimeEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false, updatable = true)
    var lastModifiedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "deleted_at", nullable = true, updatable = true)
    var deletedAt: LocalDateTime? = null

    fun markAsDeleted() {
        this.deletedAt = LocalDateTime.now()
    }
}
