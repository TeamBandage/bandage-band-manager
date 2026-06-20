package com.bandage.bandmanager.domain.setlist.service

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistBand
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class SetlistServiceCleanupTest {
    private val setlistRepository = mock(SetlistRepository::class.java)
    private val setlistBandRepository = mock(SetlistBandRepository::class.java)
    private val setlistTrackRepository = mock(SetlistTrackRepository::class.java)
    private val setlistTrackParticipantRepository = mock(SetlistTrackParticipantRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)

    private val sut =
        SetlistService(
            setlistRepository,
            setlistBandRepository,
            setlistTrackRepository,
            setlistTrackParticipantRepository,
            bandMemberRepository,
        )

    private val setlistId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val trackId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000b1")
    private val bandId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000c1")
    private val managerId = 1L
    private val base: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

    private fun setlist(): Setlist {
        val setlist = Setlist.create(trackSelectionId = UUID.randomUUID(), title = "셋리스트", managerId = managerId)
        setEntityId(setlist, setlistId)
        `when`(setlistRepository.findAllByManagerId(managerId)).thenReturn(listOf(setlist))
        return setlist
    }

    private fun track(setlist: Setlist): SetlistTrack {
        val track = SetlistTrack.create(setlist, TrackInfo(title = "곡", artist = "아티스트"), note = null, sessions = emptyList())
        setEntityId(track, trackId)
        return track
    }

    private fun participant(
        track: SetlistTrack,
        memberId: Long,
        createdAt: LocalDateTime,
    ): SetlistTrackParticipant {
        val participant = SetlistTrackParticipant.create(track, sessionId = "vocal", memberId = memberId)
        participant.createdAt = createdAt
        return participant
    }

    private fun band(): Band {
        val band = Band.create(name = "밴드", description = "설명", profileImg = null)
        setEntityId(band, bandId)
        return band
    }

    private fun bandMember(
        band: Band,
        memberId: Long,
        createdAt: LocalDateTime,
    ): BandMember {
        val bandMember = BandMember.create(band, memberId, BandRole.MEMBER)
        bandMember.createdAt = createdAt
        return bandMember
    }

    private fun setEntityId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }

    @Test
    fun `매니저 탈퇴 시 셋리스트 참여자 최고참이 매니저를 승계한다`() {
        val setlist = setlist()
        val track = track(setlist)
        `when`(setlistTrackRepository.findAllBySetlistIdIn(listOf(setlistId))).thenReturn(listOf(track))
        `when`(setlistTrackParticipantRepository.findAllByTrackIn(listOf(track)))
            .thenReturn(listOf(participant(track, 5L, base.plusDays(2)), participant(track, 6L, base.plusDays(1))))

        sut.cleanupOnWithdrawal(managerId)

        assertThat(setlist.managerId).isEqualTo(6L)
        assertThat(setlist.deletedAt).isNull()
    }

    @Test
    fun `참여자가 없으면 연결된 밴드의 최고참 멤버가 매니저를 승계한다`() {
        val setlist = setlist()
        `when`(setlistBandRepository.findAllBySetlistIdIn(listOf(setlistId)))
            .thenReturn(listOf(SetlistBand.create(bandId, setlistId)))
        val band = band()
        `when`(bandMemberRepository.findAllByBandIdIn(listOf(bandId)))
            .thenReturn(listOf(bandMember(band, 11L, base.plusDays(1)), bandMember(band, 10L, base)))

        sut.cleanupOnWithdrawal(managerId)

        assertThat(setlist.managerId).isEqualTo(10L)
        assertThat(setlist.deletedAt).isNull()
    }

    @Test
    fun `후보가 전무하면 셋리스트가 소프트 삭제된다`() {
        val setlist = setlist()

        sut.cleanupOnWithdrawal(managerId)

        assertThat(setlist.deletedAt).isNotNull()
    }
}
