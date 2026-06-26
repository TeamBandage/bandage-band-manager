package com.bandage.bandmanager.domain.selection.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemApplicant
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemConfirmation
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class TrackSelectionItemResponseTest {
    private fun item(
        proposerId: Long,
        sessionIds: List<String>,
    ): TrackSelectionItem {
        val selection = mock(TrackSelection::class.java)
        `when`(selection.id).thenReturn(UUID.randomUUID())
        val item = mock(TrackSelectionItem::class.java)
        `when`(item.id).thenReturn(UUID.randomUUID())
        `when`(item.selection).thenReturn(selection)
        `when`(item.trackInfo).thenReturn(TrackInfo(title = "곡", artist = "아티스트"))
        `when`(item.proposerId).thenReturn(proposerId)
        `when`(item.note).thenReturn(null)
        `when`(item.isSelected).thenReturn(false)
        `when`(item.sessions).thenReturn(sessionIds.map { SessionDef(it, it, it, 1, false) })
        return item
    }

    private fun applicant(
        sessionId: String,
        memberId: Long,
    ): TrackSelectionItemApplicant {
        val a = mock(TrackSelectionItemApplicant::class.java)
        `when`(a.sessionId).thenReturn(sessionId)
        `when`(a.memberId).thenReturn(memberId)
        return a
    }

    private fun confirmation(
        sessionId: String,
        memberId: Long,
    ): TrackSelectionItemConfirmation {
        val c = mock(TrackSelectionItemConfirmation::class.java)
        `when`(c.sessionId).thenReturn(sessionId)
        `when`(c.memberId).thenReturn(memberId)
        return c
    }

    @Test
    fun `of - proposer, 세션별 지원자, 확정자에 회원 정보를 채운다`() {
        val item = item(proposerId = 1L, sessionIds = listOf("G"))
        val members =
            mapOf(
                1L to MemberSummary(1L, "제안자", null),
                2L to MemberSummary(2L, "지원자", null),
                3L to MemberSummary(3L, "확정자", null),
            )

        val res =
            TrackSelectionItemResponse.of(
                item = item,
                applicants = listOf(applicant("G", 2L)),
                confirmations = listOf(confirmation("G", 3L)),
                memberInfos = members,
            )

        assertThat(res.proposer?.name).isEqualTo("제안자")
        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.applicants.map { it.name }).containsExactly("지원자")
        assertThat(guitar.confirmed.map { it.name }).containsExactly("확정자")
    }

    @Test
    fun `of - 맵에 없는 회원(탈퇴)은 proposer null, 세션 목록에서 제외`() {
        val item = item(proposerId = 99L, sessionIds = listOf("G"))
        val members = mapOf(2L to MemberSummary(2L, "지원자", null)) // 99L(제안자), 3L(확정자) 누락

        val res =
            TrackSelectionItemResponse.of(
                item = item,
                applicants = listOf(applicant("G", 2L)),
                confirmations = listOf(confirmation("G", 3L)),
                memberInfos = members,
            )

        assertThat(res.proposer).isNull()
        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.applicants.map { it.memberId }).containsExactly(2L)
        assertThat(guitar.confirmed).isEmpty() // 3L 누락 → mapNotNull 로 제외
    }
}
