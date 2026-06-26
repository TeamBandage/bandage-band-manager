package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.selection.model.TrackSelectionMember
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ParticipantResponseTest {
    private fun member(memberId: Long): TrackSelectionMember {
        val m = mock(TrackSelectionMember::class.java)
        `when`(m.memberId).thenReturn(memberId)
        `when`(m.bandIds).thenReturn(emptySet())
        return m
    }

    @Test
    fun `of - managerId와 같으면 isManager=true, 회원 정보를 채운다`() {
        val res = ParticipantResponse.of(member(1L), MemberSummary(1L, "홍길동", null), managerId = 1L)

        assertThat(res.isManager).isTrue()
        assertThat(res.memberId).isEqualTo(1L)
        assertThat(res.member?.name).isEqualTo("홍길동")
    }

    @Test
    fun `of - managerId와 다르면 isManager=false`() {
        val res = ParticipantResponse.of(member(2L), MemberSummary(2L, "김철수", null), managerId = 1L)

        assertThat(res.isManager).isFalse()
    }

    @Test
    fun `of - 회원 정보가 없으면(탈퇴) member는 null이지만 memberId와 isManager는 유지`() {
        val res = ParticipantResponse.of(member(1L), null, managerId = 1L)

        assertThat(res.member).isNull()
        assertThat(res.memberId).isEqualTo(1L)
        assertThat(res.isManager).isTrue()
    }
}
