package com.bandage.bandmanager.domain.jam.dto.res

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TimeInfoUnit
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class JamDetailResponseTest {
    // id 가 @GeneratedValue(lateinit, protected set) 라 직접 주입 불가 → mock 으로 엔티티를 구성한다.
    private fun participant(
        sessionId: String,
        member: Long,
    ): JamParticipant {
        val p = mock(JamParticipant::class.java)
        `when`(p.id).thenReturn(UUID.randomUUID())
        `when`(p.sessionId).thenReturn(sessionId)
        `when`(p.member).thenReturn(member)
        return p
    }

    private fun jamWith(participants: List<JamParticipant>): Jam {
        val sessionIds = participants.map { it.sessionId!! }.distinct()
        val jam = mock(Jam::class.java)
        `when`(jam.id).thenReturn(UUID.randomUUID())
        `when`(jam.title).thenReturn("합주")
        `when`(jam.setlistId).thenReturn(null)
        `when`(jam.note).thenReturn(null)
        `when`(jam.timeInfo).thenReturn(TimeInfoUnit(LocalDateTime.of(2026, 6, 10, 19, 0), 120, null))
        `when`(jam.trackInfo).thenReturn(TrackInfo(title = "곡", artist = "아티스트"))
        `when`(jam.sessions).thenReturn(sessionIds.map { SessionDef(it, it, it, 1, false) })
        `when`(jam.participants).thenReturn(participants)
        return jam
    }

    @Test
    fun `of - 참여자와 세션에 회원 요약 정보를 채운다`() {
        val jam = jamWith(listOf(participant("G", 1L), participant("B", 2L)))
        val members =
            mapOf(
                1L to MemberSummary(1L, "홍길동", "https://cdn/1.jpg"),
                2L to MemberSummary(2L, "김철수", null),
            )

        val res = JamDetailResponse.of(jam, members)

        assertThat(res.participants).hasSize(2)
        assertThat(res.participants.map { it.member?.name }).containsExactlyInAnyOrder("홍길동", "김철수")
        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.participants).hasSize(1)
        assertThat(guitar.participants.first().name).isEqualTo("홍길동")
    }

    @Test
    fun `of - 맵에 없는 회원(탈퇴)은 participant member가 null, 세션 목록에서는 제외된다`() {
        val jam = jamWith(listOf(participant("G", 1L), participant("G", 99L)))
        val members = mapOf(1L to MemberSummary(1L, "홍길동", null)) // 99L 누락

        val res = JamDetailResponse.of(jam, members)

        // participants 목록에는 탈퇴 회원도 행으로 남되 member=null
        assertThat(res.participants).hasSize(2)
        assertThat(res.participants.count { it.member == null }).isEqualTo(1)
        // 세션 배정 목록에서는 mapNotNull 로 누락 회원 제외
        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.participants.map { it.memberId }).containsExactly(1L)
    }
}
