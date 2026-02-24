package com.bandage.v1.domain.band.model

import com.bandage.v1.domain.band.model.enums.ApplicationStatus
import com.bandage.v1.global.common.exception.errorcode.ErrorCode
import com.bandage.v1.global.common.exception.exception.BusinessException
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", nullable = false)
    val band: Band,
    @Column(name = "member_id", nullable = false)
    val member: Long,
    @Column(name = "status", nullable = false)
    var status: ApplicationStatus? = ApplicationStatus.PENDING,
) {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
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
        require(newStatus != this.status, throw BusinessException(ErrorCode.INVALID_INPUT_VALUE))
        this.status = newStatus
    }
}
