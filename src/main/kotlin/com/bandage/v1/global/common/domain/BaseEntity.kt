package com.bandage.v1.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@MappedSuperclass
open class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @CreatedBy
    @Column(name = "created_by")
    val createdBy: String? = null
        protected set

    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    var lastModifiedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedBy
    @Column(name = "last_modified_by")
    var lastModifiedBy: String? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null

    @Column(name = "deleted_by")
    var deletedBy: String? = null
        protected set

    fun softDelete(deleter: String) {
        this.deletedAt = LocalDateTime.now()
        this.deletedBy = deleter
    }

    fun restore() {
        this.deletedAt = null
        this.deletedBy = null
    }
}
