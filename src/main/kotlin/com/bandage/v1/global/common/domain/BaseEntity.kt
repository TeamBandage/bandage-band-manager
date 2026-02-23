package com.bandage.v1.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@MappedSuperclass
class BaseEntity {
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    var createdDate: LocalDateTime = LocalDateTime.now()
        protected set

    @CreatedBy
    @Column(name = "created_by")
    var createdBy: String? = null
        protected set

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    var lastModifiedDate: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedBy
    @Column(name = "last_modified_by")
    var lastModifiedBy: String? = null
        protected set
}
