package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceInvitation
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceInvitationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformanceInvitationRepository : JpaRepository<PerformanceInvitation, UUID> {
    fun existsByPerformanceAndInvitedMemberAndStatus(
        performance: Performance,
        invitedMember: Long,
        status: PerformanceInvitationStatus,
    ): Boolean

    fun findByIdAndStatus(
        id: UUID,
        status: PerformanceInvitationStatus,
    ): PerformanceInvitation?

    fun findAllByPerformanceOrderByCreatedAtDesc(performance: Performance): List<PerformanceInvitation>

    fun findAllByInvitedMemberAndStatusOrderByCreatedAtDesc(
        invitedMember: Long,
        status: PerformanceInvitationStatus,
    ): List<PerformanceInvitation>
}
