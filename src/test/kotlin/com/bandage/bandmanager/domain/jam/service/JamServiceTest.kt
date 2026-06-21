package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.jam.dto.req.JamVenueUpdateRequest
import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class JamServiceTest {
    private val jamRepository = mock(JamRepository::class.java)
    private val jamParticipantRepository = mock(JamParticipantRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val jamReservationSyncService = mock(JamReservationSyncService::class.java)
    private val sut =
        JamService(
            jamRepository,
            jamParticipantRepository,
            bandMemberRepository,
            jamReservationSyncService,
        )

    private val jamId = UUID.randomUUID()

    private fun jam(): Jam =
        Jam.create(
            title = "합주",
            trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
            startAt = LocalDateTime.of(2026, 6, 10, 19, 0),
            durationMinutes = 120,
            venue = null,
        )

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
}
