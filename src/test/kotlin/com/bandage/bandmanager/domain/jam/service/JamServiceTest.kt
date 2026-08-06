package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.dto.req.JamMemberAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionAddRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamSessionUpdateRequest
import com.bandage.bandmanager.domain.jam.dto.req.JamVenueUpdateRequest
import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.jam.repository.JamParticipantSessionRepository
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.member.service.MemberService
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class JamServiceTest {
    private val jamRepository = mock(JamRepository::class.java)
    private val jamParticipantRepository = mock(JamParticipantRepository::class.java)
    private val jamParticipantSessionRepository = mock(JamParticipantSessionRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val jamReservationSyncService = mock(JamReservationSyncService::class.java)
    private val memberService = mock(MemberService::class.java)
    private val sut =
        JamService(
            jamRepository,
            jamParticipantRepository,
            jamParticipantSessionRepository,
            bandMemberRepository,
            jamReservationSyncService,
            memberService,
        )

    private val jamId = UUID.randomUUID()

    private fun jam(sessions: List<SessionDef> = emptyList()): Jam {
        val jam =
            Jam.create(
                title = "합주",
                trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
                startAt = LocalDateTime.of(2026, 6, 10, 19, 0),
                durationMinutes = 120,
                venue = null,
                sessions = sessions,
            )
        val field = Jam::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(jam, jamId)
        return jam
    }

    @Test
    fun `합주 참여자가 아니면 합주를 수정할 수 없다`() {
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam()))
        // existsByJamAndMember 미스텁 → 기본 false (비참여자)

        assertThatThrownBy { sut.updateVenue(jamId, JamVenueUpdateRequest("홍대 스튜디오"), 99L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_FORBIDDEN_NOT_PARTICIPANT)
    }

    @Test
    fun `합주 참여자면 합주를 수정할 수 있다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.updateVenue(jamId, JamVenueUpdateRequest("홍대 스튜디오"), 1L)

        assertThat(jam.timeInfo.venue).isEqualTo("홍대 스튜디오")
    }

    @Test
    fun `세션을 추가할 수 있다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.addSession(jamId, JamSessionAddRequest("GUITAR", false), 1L)

        // sessionId 는 서버가 발급한다(BD-269) — 값 자체가 아니라 발급 여부를 검증한다.
        assertThat(jam.sessions).singleElement()
        assertThat(jam.sessions.first().sessionId).isNotBlank()
        assertThat(jam.sessions.first().label).isEqualTo("GUITAR")
    }

    @Test
    fun `세션을 추가하면 목록 전체의 약어가 재생성된다`() {
        // 동일 이름이 2개가 되므로 기존 세션의 약어도 G -> G1 로 바뀐다(BD-229)
        val jam = jam(listOf(SessionDef("G-1", "GUITAR", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.addSession(jamId, JamSessionAddRequest("guitar", false), 1L)

        // 기존 세션의 sessionId 는 보존되고, 새 세션만 서버가 발급한다(BD-269).
        assertThat(jam.sessions.map { it.sessionId }).hasSize(2).startsWith("G-1").doesNotHaveDuplicates()
        assertThat(jam.sessions.map { it.label }).containsExactly("GUITAR", "GUITAR")
        assertThat(jam.sessions.map { it.short }).containsExactly("G1", "G2")
    }

    @Test
    fun `알파벳이 아닌 세션 이름은 추가할 수 없다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        assertThatThrownBy { sut.addSession(jamId, JamSessionAddRequest("기타", false), 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_LABEL_NOT_ALPHABETIC)
    }

    @Test
    fun `세션을 여러 번 추가해도 sessionId 는 서로 겹치지 않는다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.addSession(jamId, JamSessionAddRequest("GUITAR", false), 1L)
        sut.addSession(jamId, JamSessionAddRequest("GUITAR", false), 1L)

        assertThat(jam.sessions.map { it.sessionId }).hasSize(2).doesNotHaveDuplicates()
    }

    @Test
    fun `세션 이름을 개별로 수정하면 약어가 재생성된다`() {
        val jam = jam(listOf(SessionDef("G-1", "GUITAR", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.updateSession(jamId, "G-1", JamSessionUpdateRequest("MAINGUITAR"), 1L)

        val session = jam.sessions.first { it.sessionId == "G-1" }
        assertThat(session.label).isEqualTo("MAINGUITAR")
        assertThat(session.short).isEqualTo("M")
    }

    @Test
    fun `존재하지 않는 세션은 수정할 수 없다`() {
        val jam = jam()
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        assertThatThrownBy { sut.updateSession(jamId, "G-1", JamSessionUpdateRequest("GUITAR"), 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_SESSION_NOT_FOUND)
    }

    @Test
    fun `세션을 삭제하면 해당 세션에 대한 참여자 배정만 해제되고 참여자는 유지된다`() {
        val jam = jam(listOf(SessionDef("G-1", "GUITAR", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)

        sut.removeSession(jamId, "G-1", 1L)

        assertThat(jam.sessions).isEmpty()
        verify(jamParticipantSessionRepository).deleteAllByJamParticipantJamAndSessionIdIn(jam, setOf("G-1"))
    }

    @Test
    fun `이미 다른 멤버가 배정된 세션에는 추가로 배정할 수 없다`() {
        val jam = jam(listOf(SessionDef("G-1", "GUITAR", "G", false)))
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)
        `when`(jamParticipantSessionRepository.existsByJamParticipantJamAndSessionId(jam, "G-1")).thenReturn(true)

        assertThatThrownBy { sut.addParticipant(jamId, JamMemberAddRequest("G-1", 2L), 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JAM_SESSION_FULL)
    }

    @Test
    fun `한 참여자가 여러 세션에 배정될 수 있다`() {
        val jam = jam(listOf(SessionDef("G-1", "GUITAR", "G", false), SessionDef("V-1", "VOCAL", "V", false)))
        val participant = JamParticipant.create(jam = jam, member = 2L)
        val idField = JamParticipant::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(participant, UUID.randomUUID())
        `when`(jamRepository.findById(jamId)).thenReturn(Optional.of(jam))
        `when`(jamParticipantRepository.existsByJamAndMember(jam, 1L)).thenReturn(true)
        `when`(jamParticipantRepository.findAllByJam(jam)).thenReturn(listOf(participant))

        sut.addParticipant(jamId, JamMemberAddRequest("G-1", 2L), 1L)
        sut.addParticipant(jamId, JamMemberAddRequest("V-1", 2L), 1L)

        assertThat(participant.sessions.map { it.sessionId }).containsExactlyInAnyOrder("G-1", "V-1")
    }
}
