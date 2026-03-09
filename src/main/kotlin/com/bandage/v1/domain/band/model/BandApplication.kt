package com.bandage.v1.domain.band.model

import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import com.bandage.v1.global.common.domain.BaseEntity
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_band_application")
@SQLRestriction("deleted_at IS NULL")
open class BandApplication(
    band: Band,
    member: Long,
    status: ApplicationStatus = ApplicationStatus.PENDING,
) : BaseEntity() {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    val band: Band = band

    @Column(name = "member_id", nullable = false)
    val member: Long = member

    @Column(name = "status", nullable = false)
    var status: ApplicationStatus = status
        protected set

    @Column(name = "processed_by")
    var processedBy: Long? = null
        protected set

    companion object {
        fun create(
            band: Band,
            member: Long,
        ): BandApplication =
            BandApplication(
                band = band,
                member = member,
            )
    }

    fun updateStatus(newStatus: ApplicationStatus) {
        if (newStatus == this.status) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
        this.status = newStatus
    }
}
