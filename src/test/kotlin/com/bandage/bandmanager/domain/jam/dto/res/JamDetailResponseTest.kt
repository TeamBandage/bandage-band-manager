package com.bandage.bandmanager.domain.jam.dto.res

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.jam.model.JamParticipantSession
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
        sessionIds: List<String>,
        member: Long,
    ): JamParticipant {
        val p = mock(JamParticipant::class.java)
        `when`(p.id).thenReturn(UUID.randomUUID())
        `when`(p.member).thenReturn(member)
        val sessions =
            sessionIds.map { sessionId ->
                val session = mock(JamParticipantSession::class.java)
                `when`(session.sessionId).thenReturn(sessionId)
                session
            }
        `when`(p.sessions).thenReturn(sessions)
        return p
    }

    private fun jamWith(participants: List<JamParticipant>): Jam {
        val sessionIds = participants.flatMap { it.sessions.map { s -> s.sessionId } }.distinct()
        val jam = mock(Jam::class.java)
        `when`(jam.id).thenReturn(UUID.randomUUID())
        `when`(jam.title).thenReturn("합주")
        `when`(jam.setlistId).thenReturn(null)
        `when`(jam.note).thenReturn(null)
        `when`(jam.timeInfo).thenReturn(TimeInfoUnit(LocalDateTime.of(2026, 6, 10, 19, 0), 120, null))
        `when`(jam.trackInfo).thenReturn(TrackInfo(title = "곡", artist = "아티스트"))
        `when`(jam.sessions).thenReturn(sessionIds.map { SessionDef(it, it, it, false) })
        `when`(jam.participants).thenReturn(participants)
        return jam
    }

    @Test
    fun `of - 참여자와 세션에 회원 요약 정보를 채운다`() {
        val jam = jamWith(listOf(participant(listOf("G"), 1L), participant(listOf("B"), 2L)))
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
        val jam = jamWith(listOf(participant(listOf("G"), 1L), participant(listOf("G"), 99L)))
        val members = mapOf(1L to MemberSummary(1L, "홍길동", null)) // 99L 누락

        val res = JamDetailResponse.of(jam, members)

        // participants 목록에는 탈퇴 회원도 행으로 남되 member=null
        assertThat(res.participants).hasSize(2)
        assertThat(res.participants.count { it.member == null }).isEqualTo(1)
        // 세션 배정 목록에서는 filterNotNull 로 누락 회원 제외
        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.participants.map { it.memberId }).containsExactly(1L)
    }

    @Test
    fun `of - 한 참여자가 여러 세션에 배정되면 각 세션 응답에 모두 노출된다`() {
        val jam = jamWith(listOf(participant(listOf("G", "V"), 1L)))
        val members = mapOf(1L to MemberSummary(1L, "홍길동", null))

        val res = JamDetailResponse.of(jam, members)

        assertThat(res.participants).hasSize(1)
        assertThat(res.participants.first().sessionIds).containsExactlyInAnyOrder("G", "V")
        assertThat(
            res.sessions
                .first { it.sessionId == "G" }
                .participants
                .map { it.memberId },
        ).containsExactly(1L)
        assertThat(
            res.sessions
                .first { it.sessionId == "V" }
                .participants
                .map { it.memberId },
        ).containsExactly(1L)
    }
}
