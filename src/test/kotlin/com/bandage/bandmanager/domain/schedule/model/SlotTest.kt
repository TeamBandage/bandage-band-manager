package com.bandage.bandmanager.domain.schedule.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotTest {
    private val day: LocalDate = LocalDate.of(2026, 8, 17)

    @Test
    fun `같은 날 구간의 총 슬롯 수를 센다`() {
        assertEquals(4, Slot.of(day, 36, day, 40).totalSlots)
    }

    @Test
    fun `자정을 넘기는 구간은 다음 날 endSlot 으로 표현한다`() {
        assertEquals(2, Slot.of(day, 47, day.plusDays(1), 1).totalSlots)
    }

    @Test
    fun `끝이 시작보다 앞서면 거부한다`() {
        assertThrows<IllegalArgumentException> { Slot.of(day, 40, day, 36) }
    }

    @Test
    fun `길이가 0인 구간은 거부한다`() {
        assertThrows<IllegalArgumentException> { Slot.of(day, 36, day, 36) }
    }

    @Test
    fun `슬롯 범위를 벗어나면 거부한다`() {
        assertThrows<IllegalArgumentException> { Slot.of(day, 0, day, 48) }
        assertThrows<IllegalArgumentException> { Slot.of(day, -1, day, 10) }
    }

    @Test
    fun `겹치는 구간을 판정한다`() {
        val base = Slot.of(day, 36, day, 40)
        assertTrue(base.overlaps(Slot.of(day, 38, day, 42)))
        assertTrue(base.overlaps(Slot.of(day, 30, day, 46)))
    }

    @Test
    fun `경계가 맞닿은 구간은 겹치지 않는다`() {
        val base = Slot.of(day, 36, day, 40)
        assertFalse(base.overlaps(Slot.of(day, 40, day, 44)))
        assertFalse(base.overlaps(Slot.of(day, 32, day, 36)))
    }

    @Test
    fun `날짜가 다르면 슬롯 번호가 같아도 겹치지 않는다`() {
        val base = Slot.of(day, 36, day, 40)
        assertFalse(base.overlaps(Slot.of(day.plusDays(1), 36, day.plusDays(1), 40)))
    }
}
