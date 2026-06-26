package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class SetlistChatMessageResponseTest {
    private fun chatMessage(): TrackSelectionItemChatMessage {
        val item = mock(TrackSelectionItem::class.java)
        `when`(item.id).thenReturn(UUID.randomUUID())
        val c = mock(TrackSelectionItemChatMessage::class.java)
        `when`(c.id).thenReturn(UUID.randomUUID())
        `when`(c.item).thenReturn(item)
        `when`(c.message).thenReturn("안녕하세요")
        return c
    }

    @Test
    fun `of - 작성자 회원 정보를 채운다`() {
        val res = SetlistChatMessageResponse.of(chatMessage(), MemberSummary(1L, "작성자", "https://cdn/1.jpg"))

        assertThat(res.member?.memberId).isEqualTo(1L)
        assertThat(res.member?.name).isEqualTo("작성자")
        assertThat(res.message).isEqualTo("안녕하세요")
    }

    @Test
    fun `of - 탈퇴 회원이면 member는 null`() {
        val res = SetlistChatMessageResponse.of(chatMessage(), null)

        assertThat(res.member).isNull()
        assertThat(res.message).isEqualTo("안녕하세요")
    }
}
