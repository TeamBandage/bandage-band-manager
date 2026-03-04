package com.bandage.v1.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import java.time.LocalDateTime

@MappedSuperclass
open class BaseEntity : BaseTimeEntity() {
    @CreatedBy
    @Column(name = "created_by", nullable = true, updatable = false)
    val createdBy: Long? = null

    @LastModifiedBy
    @Column(name = "last_modified_by", nullable = true, updatable = true)
    var lastModifiedBy: Long? = null
        protected set

    @Column(name = "deleted_by", nullable = true, updatable = false)
    var deletedBy: Long? = null
        protected set

    fun markAsDeleted(deleter: Long) {
        super.deletedAt = LocalDateTime.now()
        this.deletedBy = deleter
    }

    fun restore() {
        super.deletedAt = null
        this.deletedBy = null
    }
}
