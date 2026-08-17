package com.bandage.bandmanager.domain.schedule.repository

import com.bandage.bandmanager.domain.schedule.model.ScheduleBlock
import com.bandage.bandmanager.domain.schedule.model.ScheduleBlockTrack
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.model.Slot
import com.bandage.bandmanager.global.config.querydsl.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.UUID

/**
 * 트랙별 배치 현황 집계(BD-272) 검증.
 *
 * 별도 적재 테이블 없이 ScheduleBlockTrack 을 집계하는 것이 요점이므로,
 * 수동/자동 구분 없이 블록이 생기면 그대로 집계에 잡히는지와 보드가 섞이지 않는지를 본다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class ScheduleBlockTrackPlacementCountTest {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var sut: ScheduleBlockTrackRepository

    private lateinit var board: ScheduleBoard
    private val day: LocalDate = LocalDate.of(2026, 9, 1)

    @BeforeEach
    fun setUp() {
        board = persist(ScheduleBoard.create(setlistId = UUID.randomUUID(), name = "시안"))
    }

    @Test
    fun `트랙별로 배치된 블록 수를 집계한다`() {
        val trackA = UUID.randomUUID()
        val trackB = UUID.randomUUID()
        blockWith(trackA, startSlot = 36)
        blockWith(trackA, startSlot = 40)
        blockWith(trackB, startSlot = 44)

        val counts = sut.countPlacementsByBoardId(board.id).associate { it.getTrackId() to it.getPlacementCount() }

        assertThat(counts[trackA]).isEqualTo(2L)
        assertThat(counts[trackB]).isEqualTo(1L)
    }

    @Test
    fun `배치된 적 없는 트랙은 집계 결과에 나타나지 않는다`() {
        val placed = UUID.randomUUID()
        val neverPlaced = UUID.randomUUID()
        blockWith(placed, startSlot = 36)

        val counts = sut.countPlacementsByBoardId(board.id).associate { it.getTrackId() to it.getPlacementCount() }

        assertThat(counts).containsOnlyKeys(placed)
        assertThat(counts[neverPlaced]).isNull()
    }

    @Test
    fun `다른 보드의 배치는 섞이지 않는다`() {
        val other = persist(ScheduleBoard.create(setlistId = UUID.randomUUID(), name = "다른 시안"))
        val track = UUID.randomUUID()
        blockWith(track, startSlot = 36)
        blockWith(track, startSlot = 40, targetBoard = other)

        val counts = sut.countPlacementsByBoardId(board.id).associate { it.getTrackId() to it.getPlacementCount() }

        assertThat(counts[track]).isEqualTo(1L)
    }

    @Test
    fun `블록이 하나도 없으면 빈 결과를 반환한다`() {
        assertThat(sut.countPlacementsByBoardId(board.id)).isEmpty()
    }

    /** 한 블록에 트랙이 여러 개 붙는 수동 묶음도 트랙마다 1회로 집계된다. */
    @Test
    fun `한 블록에 묶인 트랙들은 각각 1회로 집계된다`() {
        val trackA = UUID.randomUUID()
        val trackB = UUID.randomUUID()
        val block = persist(ScheduleBlock.create(board = board, slot = Slot.ofDayRange(day, 36, 40)))
        persist(ScheduleBlockTrack.create(block = block, setlistTrackId = trackA, ordinal = 0))
        persist(ScheduleBlockTrack.create(block = block, setlistTrackId = trackB, ordinal = 1))

        val counts = sut.countPlacementsByBoardId(board.id).associate { it.getTrackId() to it.getPlacementCount() }

        assertThat(counts[trackA]).isEqualTo(1L)
        assertThat(counts[trackB]).isEqualTo(1L)
    }

    private fun blockWith(
        trackId: UUID,
        startSlot: Int,
        targetBoard: ScheduleBoard = board,
    ) {
        val block =
            persist(
                ScheduleBlock.create(
                    board = targetBoard,
                    slot = Slot.ofDayRange(day, startSlot, startSlot + 2),
                ),
            )
        persist(ScheduleBlockTrack.create(block = block, setlistTrackId = trackId, ordinal = 0))
    }

    private fun <T : Any> persist(entity: T): T {
        em.persist(entity)
        em.flush()
        return entity
    }
}
