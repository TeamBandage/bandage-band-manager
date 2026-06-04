package com.bandage.v1.domain.performance.model

import com.bandage.v1.domain.performance.model.enums.PerformanceInvitationStatus
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
@Table(name = "p_performance_invitation")
@SQLRestriction("deleted_at IS NULL")
open class PerformanceInvitation(
    performance: Performance,
    invitedMember: Long,
    invitedBy: Long,
    status: PerformanceInvitationStatus = PerformanceInvitationStatus.PENDING,
) : BaseEntity() {
    @Id
    @Column(name = "performance_invitation_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    val performance: Performance = performance

    @Column(name = "invited_member_id", nullable = false)
    val invitedMember: Long = invitedMember

    @Column(name = "invited_by", nullable = false)
    val invitedBy: Long = invitedBy

    @Column(name = "status", nullable = false)
    var status: PerformanceInvitationStatus = status
        protected set

    @Column(name = "processed_by")
    var processedBy: Long? = null
        protected set

    fun updateStatus(newStatus: PerformanceInvitationStatus) {
        if (newStatus == this.status) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }
        this.status = newStatus
    }

    fun markProcessedBy(memberId: Long) {
        this.processedBy = memberId
    }

    companion object {
        fun create(
            performance: Performance,
            invitedMember: Long,
            invitedBy: Long,
        ): PerformanceInvitation =
            PerformanceInvitation(
                performance = performance,
                invitedMember = invitedMember,
                invitedBy = invitedBy,
            )
    }
}
