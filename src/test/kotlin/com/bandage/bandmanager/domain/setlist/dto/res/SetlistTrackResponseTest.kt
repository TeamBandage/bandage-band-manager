package com.bandage.bandmanager.domain.setlist.dto.res

import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class SetlistTrackResponseTest {
    private fun track(sessionIds: List<String>): SetlistTrack {
        val setlist = mock(Setlist::class.java)
        `when`(setlist.id).thenReturn(UUID.randomUUID())
        val t = mock(SetlistTrack::class.java)
        `when`(t.id).thenReturn(UUID.randomUUID())
        `when`(t.setlist).thenReturn(setlist)
        `when`(t.trackInfo).thenReturn(TrackInfo(title = "곡", artist = "아티스트"))
        `when`(t.note).thenReturn(null)
        `when`(t.sessions).thenReturn(sessionIds.map { SessionDef(it, it, it, 1, false) })
        return t
    }

    private fun participant(
        sessionId: String,
        memberId: Long,
    ): SetlistTrackParticipant {
        val p = mock(SetlistTrackParticipant::class.java)
        `when`(p.sessionId).thenReturn(sessionId)
        `when`(p.memberId).thenReturn(memberId)
        return p
    }

    @Test
    fun `of - 세션별 참여자에 회원 정보를 채운다`() {
        val track = track(listOf("G"))
        val members = mapOf(2L to MemberSummary(2L, "참여자", "https://cdn/2.jpg"))

        val res = SetlistTrackResponse.of(track, listOf(participant("G", 2L)), members)

        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.participants).hasSize(1)
        assertThat(guitar.participants.first().name).isEqualTo("참여자")
    }

    @Test
    fun `of - 맵에 없는 회원(탈퇴)은 세션 목록에서 제외`() {
        val track = track(listOf("G"))
        val members = mapOf(2L to MemberSummary(2L, "참여자", null)) // 99L 누락

        val res = SetlistTrackResponse.of(track, listOf(participant("G", 2L), participant("G", 99L)), members)

        val guitar = res.sessions.first { it.sessionId == "G" }
        assertThat(guitar.participants.map { it.memberId }).containsExactly(2L)
    }
}
