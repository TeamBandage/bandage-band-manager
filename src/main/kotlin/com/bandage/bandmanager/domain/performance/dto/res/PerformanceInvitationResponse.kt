package com.bandage.bandmanager.domain.performance.dto.res

import com.bandage.bandmanager.domain.performance.model.PerformanceInvitation
import com.bandage.bandmanager.domain.performance.model.enums.PerformanceInvitationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "공연 매니저 초대 응답")
data class PerformanceInvitationResponse(
    @Schema(description = "초대 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val invitationId: UUID,
    @Schema(description = "공연 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val performanceId: UUID,
    @Schema(description = "공연 제목", example = "정기 공연 2026")
    val performanceTitle: String,
    @Schema(description = "초대받은 멤버 ID", example = "42")
    val invitedMemberId: Long,
    @Schema(description = "초대받은 멤버 이름", example = "홍길동")
    val invitedMemberName: String?,
    @Schema(description = "초대받은 멤버 프로필 이미지 URL")
    val invitedMemberProfileImg: String?,
    @Schema(description = "초대 상태", example = "PENDING")
    val status: PerformanceInvitationStatus,
    @Schema(description = "초대 생성 시각")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(
            invitation: PerformanceInvitation,
            invitedMemberName: String?,
            invitedMemberProfileImg: String?,
        ): PerformanceInvitationResponse =
            PerformanceInvitationResponse(
                invitationId = invitation.id,
                performanceId = invitation.performance.id,
                performanceTitle = invitation.performance.title,
                invitedMemberId = invitation.invitedMember,
                invitedMemberName = invitedMemberName,
                invitedMemberProfileImg = invitedMemberProfileImg,
                status = invitation.status,
                createdAt = invitation.createdAt,
            )
    }
}
