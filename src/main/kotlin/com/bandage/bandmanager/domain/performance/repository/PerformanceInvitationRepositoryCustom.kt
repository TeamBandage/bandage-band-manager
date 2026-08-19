package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceInvitation
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceInvitationStatus
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface PerformanceInvitationRepositoryCustom {
    fun findAllByPerformanceAndPaging(
        performance: Performance,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformanceInvitation, UUID>

    fun findAllByInvitedMemberAndStatusAndPaging(
        invitedMember: Long,
        status: PerformanceInvitationStatus,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformanceInvitation, UUID>
}
