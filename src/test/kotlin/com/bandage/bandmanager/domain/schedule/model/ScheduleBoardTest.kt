package com.bandage.bandmanager.domain.schedule.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class ScheduleBoardTest {
    private fun board(
        from: Int = ScheduleBoard.DEFAULT_BOARD_TIME_RANGE_FROM,
        to: Int = ScheduleBoard.DEFAULT_BOARD_TIME_RANGE_TO,
    ): ScheduleBoard =
        ScheduleBoard.create(
            setlistId = UUID.randomUUID(),
            name = "시안",
            boardTimeRangeFrom = from,
            boardTimeRangeTo = to,
        )

    @Test
    fun `기본 시간대는 슬롯 18부터 44까지다`() {
        val created = board()
        assertEquals(18, created.boardTimeRangeFrom)
        assertEquals(44, created.boardTimeRangeTo)
    }

    @Test
    fun `자정 종료를 슬롯 48로 표현할 수 있다`() {
        assertEquals(48, board(from = 0, to = 48).boardTimeRangeTo)
    }

    @Test
    fun `시작이 끝보다 뒤면 거부한다`() {
        assertThrows<IllegalArgumentException> { board(from = 44, to = 18) }
    }

    @Test
    fun `길이가 0인 시간대는 거부한다`() {
        assertThrows<IllegalArgumentException> { board(from = 18, to = 18) }
    }

    @Test
    fun `from 이 범위를 벗어나면 거부한다`() {
        assertThrows<IllegalArgumentException> { board(from = -1, to = 20) }
        assertThrows<IllegalArgumentException> { board(from = 48, to = 48) }
    }

    @Test
    fun `to 가 범위를 벗어나면 거부한다`() {
        assertThrows<IllegalArgumentException> { board(from = 0, to = 0) }
        assertThrows<IllegalArgumentException> { board(from = 0, to = 49) }
    }

    @Test
    fun `시간대를 갱신한다`() {
        val created = board()
        created.updateTimeRange(20, 40)
        assertEquals(20, created.boardTimeRangeFrom)
        assertEquals(40, created.boardTimeRangeTo)
    }

    @Test
    fun `잘못된 시간대로는 갱신되지 않는다`() {
        val created = board()
        assertThrows<IllegalArgumentException> { created.updateTimeRange(40, 20) }
        assertEquals(18, created.boardTimeRangeFrom)
        assertEquals(44, created.boardTimeRangeTo)
    }
}
