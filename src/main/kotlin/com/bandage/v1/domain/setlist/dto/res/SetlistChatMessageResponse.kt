package com.bandage.v1.domain.setlist.dto.res

import com.bandage.v1.domain.setlist.model.SetlistMeetingItemChatMessage
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 회의 항목 채팅 메시지 응답")
data class SetlistChatMessageResponse(
    val messageId: UUID,
    val setlistMeetingItemId: UUID,
    val memberId: Long,
    val message: String,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(c: SetlistMeetingItemChatMessage): SetlistChatMessageResponse =
            SetlistChatMessageResponse(
                messageId = c.id,
                setlistMeetingItemId = c.item.id,
                memberId = c.memberId,
                message = c.message,
                createdAt = c.createdAt,
            )
    }
}
