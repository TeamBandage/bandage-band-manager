package com.bandage.v1.domain.setlist.dto.res

import com.bandage.v1.domain.setlist.dto.PracticeWindowDto
import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.domain.setlist.model.enums.MeetingPurpose
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 회의 상세 응답")
data class SetlistMeetingDetailResponse(
    val meetingId: UUID,
    val bandId: UUID,
    val title: String,
    val purpose: MeetingPurpose,
    val performanceId: UUID?,
    val managerId: Long,
    val participantUserIds: List<Long>,
    val practiceWindow: PracticeWindowDto,
    val lockedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            m: SetlistMeeting,
            participantUserIds: List<Long>,
        ): SetlistMeetingDetailResponse =
            SetlistMeetingDetailResponse(
                meetingId = m.id,
                bandId = m.bandId,
                title = m.title,
                purpose = m.purpose,
                performanceId = m.performanceId,
                managerId = m.managerId,
                participantUserIds = participantUserIds,
                practiceWindow = PracticeWindowDto.of(m.practiceWindow),
                lockedAt = m.lockedAt,
                createdAt = m.createdAt,
                updatedAt = m.lastModifiedAt,
            )
    }
}
