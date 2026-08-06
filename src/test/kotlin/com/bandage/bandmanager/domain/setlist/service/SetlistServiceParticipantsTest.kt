package com.bandage.bandmanager.domain.setlist.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.member.dto.res.MemberSummary
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.domain.performance.repository.PerformanceSetlistRepository
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.SessionSpec
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

/** 셋리스트 참여자 목록 조립(BD-226, BD-180) 검증. */
class SetlistServiceParticipantsTest {
    private val setlistRepository = mock(SetlistRepository::class.java)
    private val setlistBandRepository = mock(SetlistBandRepository::class.java)
    private val setlistTrackRepository = mock(SetlistTrackRepository::class.java)
    private val setlistTrackParticipantRepository = mock(SetlistTrackParticipantRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val performanceSetlistRepository = mock(PerformanceSetlistRepository::class.java)
    private val memberService = mock(MemberService::class.java)

    private val sut =
        SetlistService(
            setlistRepository,
            setlistBandRepository,
            setlistTrackRepository,
            setlistTrackParticipantRepository,
            bandMemberRepository,
            performanceSetlistRepository,
            memberService,
        )

    private val setlistId: UUID = UUID.randomUUID()
    private val trackId: UUID = UUID.randomUUID()
    private val managerId = 1L
    private val guitaristId = 2L

    @Test
    fun `참여자별로 배정 세션과 약어를 반환하고 매니저도 포함한다`() {
        val setlist = setlist()
        val track = track(setlist, listOf("G-1" to "GUITAR", "G-2" to "GUITAR"))
        stubTracks(setlist, track)
        stubParticipants(track, listOf("G-2" to guitaristId))
        stubMembers(managerId to "매니저", guitaristId to "기타리스트")

        val result = sut.getParticipants(setlistId, managerId)

        assertThat(result).hasSize(2)
        val manager = result.first { it.isManager }
        assertThat(manager.member?.memberId).isEqualTo(managerId)
        assertThat(manager.sessions).isEmpty()

        val guitarist = result.first { !it.isManager }
        assertThat(guitarist.member?.memberId).isEqualTo(guitaristId)
        // 같은 이름 2개이므로 약어는 G1/G2 로 생성되고, G-2 에 배정되었으므로 G2
        assertThat(guitarist.sessions).singleElement().satisfies({
            assertThat(it.setlistTrackId).isEqualTo(trackId)
            assertThat(it.sessionId).isEqualTo("G-2")
            assertThat(it.label).isEqualTo("GUITAR")
            assertThat(it.short).isEqualTo("G2")
        })
    }

    @Test
    fun `한 멤버가 여러 세션에 배정되면 세션 목록으로 반환된다`() {
        val setlist = setlist()
        val track = track(setlist, listOf("V-1" to "VOCAL", "G-1" to "GUITAR"))
        stubTracks(setlist, track)
        stubParticipants(track, listOf("V-1" to guitaristId, "G-1" to guitaristId))
        stubMembers(managerId to "매니저", guitaristId to "멀티플레이어")

        val result = sut.getParticipants(setlistId, managerId)

        val multi = result.first { !it.isManager }
        assertThat(multi.sessions.map { it.short }).containsExactlyInAnyOrder("V", "G")
    }

    @Test
    fun `세션 정의가 없는 고아 배정은 세션 토큰을 표시값으로 사용한다`() {
        val setlist = setlist()
        val track = track(setlist, listOf("V-1" to "VOCAL"))
        stubTracks(setlist, track)
        // 세션 교체 후 남은 고아 배정(정의에 없는 sessionId)
        stubParticipants(track, listOf("GHOST" to guitaristId))
        stubMembers(managerId to "매니저", guitaristId to "유령")

        val result = sut.getParticipants(setlistId, managerId)

        val orphan = result.first { !it.isManager }.sessions.single()
        assertThat(orphan.label).isEqualTo("GHOST")
        assertThat(orphan.short).isEqualTo("GHOST")
    }

    @Test
    fun `트랙이 없으면 매니저만 반환된다`() {
        val setlist = setlist()
        stubTracks(setlist)
        stubMembers(managerId to "매니저")

        val result = sut.getParticipants(setlistId, managerId)

        assertThat(result).singleElement().satisfies({
            assertThat(it.isManager).isTrue()
            assertThat(it.sessions).isEmpty()
        })
    }

    @Test
    fun `접근 권한이 없으면 조회할 수 없다`() {
        setlist()
        `when`(setlistRepository.isAccessibleMember(setlistId, 99L)).thenReturn(false)

        assertThatThrownBy { sut.getParticipants(setlistId, 99L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETLIST_FORBIDDEN)
    }

    // ---------- fixtures ----------

    private fun setlist(): Setlist {
        val setlist = Setlist.create(trackSelectionId = UUID.randomUUID(), title = "셋리스트", managerId = managerId)
        setId(setlist, setlistId)
        `when`(setlistRepository.findById(setlistId)).thenReturn(Optional.of(setlist))
        return setlist
    }

    private fun track(
        setlist: Setlist,
        sessions: List<Pair<String, String>>,
    ): SetlistTrack {
        val track =
            SetlistTrack.create(
                setlist = setlist,
                trackInfo = TrackInfo(title = "트랙", artist = "아티스트"),
                note = null,
                sessions =
                    SessionDef.createAll(
                        sessions.map { (id, label) -> SessionSpec(id, label) },
                        existingSessionIds = sessions.map { it.first }.toSet(),
                    ),
            )
        setId(track, trackId)
        return track
    }

    private fun stubTracks(
        setlist: Setlist,
        vararg tracks: SetlistTrack,
    ) {
        `when`(setlistTrackRepository.findAllBySetlist(setlist)).thenReturn(tracks.toList())
    }

    private fun stubParticipants(
        track: SetlistTrack,
        assignments: List<Pair<String, Long>>,
    ) {
        val participants = assignments.map { (sessionId, memberId) -> SetlistTrackParticipant.create(track, sessionId, memberId) }
        `when`(setlistTrackParticipantRepository.findAllByTrackIn(listOf(track))).thenReturn(participants)
    }

    private fun stubMembers(vararg members: Pair<Long, String>) {
        val summaries = members.associate { (id, name) -> id to MemberSummary(memberId = id, name = name) }
        `when`(memberService.getMemberSummaries(anyCollection())).thenReturn(summaries)
    }

    private fun setId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }
}
