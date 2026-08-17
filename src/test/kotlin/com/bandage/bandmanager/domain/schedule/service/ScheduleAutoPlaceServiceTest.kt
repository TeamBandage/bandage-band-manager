package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleAutoPlaceRequest
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.model.Slot
import com.bandage.bandmanager.domain.schedule.model.enums.Frequency
import com.bandage.bandmanager.domain.schedule.model.enums.PlacementOrigin
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockTrackRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ScheduleAutoPlaceServiceTest {
    private val boardRepository = mock(ScheduleBoardRepository::class.java)
    private val blockRepository = mock(ScheduleBlockRepository::class.java)
    private val blockTrackRepository = mock(ScheduleBlockTrackRepository::class.java)
    private val trackRepository = mock(SetlistTrackRepository::class.java)
    private val participantRepository = mock(SetlistTrackParticipantRepository::class.java)
    private val availabilityRepository = mock(MemberAvailabilityRepository::class.java)
    private val authService = mock(ScheduleAuthService::class.java)
    private val sut =
        ScheduleAutoPlaceService(
            boardRepository,
            blockRepository,
            blockTrackRepository,
            trackRepository,
            participantRepository,
            ScheduleAvailabilityService(participantRepository, availabilityRepository, authService),
            authService,
        )

    private val setlistId: UUID = UUID.randomUUID()
    private val boardId: UUID = UUID.randomUUID()
    private val mon: LocalDate = LocalDate.of(2026, 6, 1)
    private val setlist = Setlist.create(trackSelectionId = UUID.randomUUID(), title = "셋리스트", managerId = 1L)

    private fun board(windowTo: LocalDate): ScheduleBoard =
        ScheduleBoard
            .create(setlistId = setlistId, name = "시안", windowFrom = mon, windowTo = windowTo)
            .withId(boardId)

    private fun track(): SetlistTrack =
        SetlistTrack
            .create(
                setlist = setlist,
                trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
                note = null,
                sessions = listOf(SessionDef("vocal", "보컬", "Vo", false)),
            ).withId(UUID.randomUUID())

    /** 월요일 슬롯 18~44 (09:00~22:00) 가용 */
    private fun mondayAvailability(memberId: Long): MemberAvailability =
        MemberAvailability.create(memberId).apply {
            updateWeeklyRules(listOf(WeeklyRule(DayOfWeek.MONDAY, startSlot = 18, endSlot = 44, effectiveFrom = mon)))
        }

    @Suppress("UNCHECKED_CAST")
    private fun stub(
        board: ScheduleBoard,
        tracks: List<SetlistTrack>,
        participants: List<SetlistTrackParticipant>,
        availabilities: List<MemberAvailability>,
        pinned: List<ScheduleBlock> = emptyList(),
        pinnedTracks: List<ScheduleBlockTrack> = emptyList(),
    ) {
        `when`(boardRepository.findById(boardId)).thenReturn(Optional.of(board))
        `when`(trackRepository.findAllBySetlistIdIn(listOf(setlistId))).thenReturn(tracks)
        `when`(participantRepository.findAllBySetlistId(setlistId)).thenReturn(participants)
        `when`(availabilityRepository.findAllByMemberIdIn(anyCollection())).thenReturn(availabilities)
        `when`(blockRepository.findAllByBoardIdAndPinned(boardId, true)).thenReturn(pinned)
        `when`(blockRepository.findAllByBoardIdAndPinned(boardId, false)).thenReturn(emptyList())
        `when`(blockTrackRepository.findAllByBlockIdIn(pinned.map { it.id })).thenReturn(pinnedTracks)
        `when`(blockRepository.saveAll(anyList())).thenAnswer { it.arguments[0] }
        `when`(blockTrackRepository.saveAll(anyList())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `같은 멤버의 두 트랙은 같은 날 인접 슬롯에 연속 배치된다`() {
        val t1 = track()
        val t2 = track()
        stub(
            board = board(windowTo = mon.plusDays(6)),
            tracks = listOf(t1, t2),
            participants = listOf(participant(t1, 1L), participant(t2, 1L)),
            availabilities = listOf(mondayAvailability(1L)),
        )

        val response = sut.autoPlaceScheduleBlocks(setlistId, boardId, 1L, ScheduleAutoPlaceRequest())

        assertThat(response.blocks).hasSize(2)
        assertThat(response.blocks.map { it.startDate }).containsOnly(mon)
        assertThat(response.blocks.map { it.startSlot to it.endSlot }).containsExactly(18 to 22, 22 to 26)
        assertThat(response.blocks.map { it.placementOrigin }).containsOnly(PlacementOrigin.AUTO)
        assertThat(response.blocks.map { it.trackIds }).containsExactly(listOf(t1.id), listOf(t2.id))
    }

    @Test
    fun `가용한 날이 없으면 배치하지 않는다`() {
        val t1 = track()
        stub(
            board = board(windowTo = mon.plusDays(6)),
            tracks = listOf(t1),
            participants = listOf(participant(t1, 1L)),
            availabilities = listOf(MemberAvailability.create(1L)),
        )

        val response = sut.autoPlaceScheduleBlocks(setlistId, boardId, 1L, ScheduleAutoPlaceRequest())

        assertThat(response.blocks).isEmpty()
    }

    @Test
    fun `고정 블록의 트랙은 재배치하지 않고 그 점유에 이어 배치한다`() {
        val t1 = track()
        val t2 = track()
        val board = board(windowTo = mon.plusDays(6))
        val pinnedBlock = ScheduleBlock.create(board = board, slot = Slot.ofDayRange(mon, 18, 26)).apply { pin() }
        stub(
            board = board,
            tracks = listOf(t1, t2),
            participants = listOf(participant(t1, 1L), participant(t2, 1L)),
            availabilities = listOf(mondayAvailability(1L)),
            pinned = listOf(pinnedBlock),
            pinnedTracks = listOf(ScheduleBlockTrack.create(block = pinnedBlock, setlistTrackId = t1.id, ordinal = 0)),
        )

        val response = sut.autoPlaceScheduleBlocks(setlistId, boardId, 1L, ScheduleAutoPlaceRequest())

        // 고정 [18,26) 보존 + t2 는 maxJamsPerDay=1 이라 고정 구간에 붙여서만 배치 가능
        assertThat(response.blocks).hasSize(2)
        val auto = response.blocks.single { it.placementOrigin == PlacementOrigin.AUTO }
        assertThat(auto.trackIds).containsExactly(t2.id)
        assertThat(auto.startSlot to auto.endSlot).isEqualTo(26 to 30)
    }

    @Test
    fun `WEEKLY 는 회차마다 배치한다`() {
        val t1 = track()
        stub(
            board = board(windowTo = mon.plusDays(13)),
            tracks = listOf(t1),
            participants = listOf(participant(t1, 1L)),
            availabilities = listOf(mondayAvailability(1L)),
        )

        val response = sut.autoPlaceScheduleBlocks(setlistId, boardId, 1L, ScheduleAutoPlaceRequest(interval = Frequency.WEEKLY))

        assertThat(response.blocks).hasSize(2)
        assertThat(response.blocks.map { it.startDate }).containsExactly(mon, mon.plusDays(7))
        assertThat(response.blocks.map { it.startSlot }).containsOnly(18)
    }

    private fun participant(
        track: SetlistTrack,
        memberId: Long,
    ): SetlistTrackParticipant = SetlistTrackParticipant.create(track, "vocal", memberId)

    private fun <T : Any> T.withId(id: UUID): T {
        javaClass.getDeclaredField("id").apply { isAccessible = true }.set(this, id)
        return this
    }
}
