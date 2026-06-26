package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "선곡 항목 채팅 메시지 응답")
data class SetlistChatMessageResponse(
    val messageId: UUID,
    val trackSelectionItemId: UUID,
    @Schema(description = "작성자 회원 정보 (탈퇴 회원이면 null)")
    val member: MemberSummary?,
    val message: String,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(
            c: TrackSelectionItemChatMessage,
            member: MemberSummary?,
        ): SetlistChatMessageResponse =
            SetlistChatMessageResponse(
                messageId = c.id,
                trackSelectionItemId = c.item.id,
                member = member,
                message = c.message,
                createdAt = c.createdAt,
            )
    }
}
