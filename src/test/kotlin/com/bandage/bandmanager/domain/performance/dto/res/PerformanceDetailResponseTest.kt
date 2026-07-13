package com.bandage.bandmanager.domain.performance.dto.res

import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformanceManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

class PerformanceDetailResponseTest {
    private val ownerId = 1L
    private val managerId = 2L

    private fun performanceWithManagers(managers: List<PerformanceManager>): Performance {
        val performance = mock(Performance::class.java)
        `when`(performance.id).thenReturn(java.util.UUID.randomUUID())
        `when`(performance.title).thenReturn("정기공연")
        `when`(performance.timeInfo).thenReturn(
            com.bandage.bandmanager.global.common.domain.TimeInfoUnit(
                startAt = LocalDateTime.now().plusDays(1),
                durationMinutes = 90,
                venue = null,
            ),
        )
        `when`(performance.setlists).thenReturn(emptyList())
        `when`(performance.managers).thenReturn(managers)
        return performance
    }

    @Test
    fun `of - OWNER와 MANAGER가 각각 ownerId, managerIds로 분리된다`() {
        val performance =
            mock(Performance::class.java).also {
                `when`(it.id).thenReturn(java.util.UUID.randomUUID())
            }
        val owner = PerformanceManager.createOwner(performance, ownerId)
        val manager = PerformanceManager.createManager(performance, managerId)
        val target = performanceWithManagers(listOf(owner, manager))

        val response = PerformanceDetailResponse.of(target, emptyMap())

        assertThat(response.ownerId).isEqualTo(ownerId)
        assertThat(response.managerIds).containsExactly(managerId)
    }

    @Test
    fun `of - OWNER가 없으면 명확한 예외를 던진다`() {
        val target = performanceWithManagers(emptyList())

        assertThrows<IllegalStateException> { PerformanceDetailResponse.of(target, emptyMap()) }
    }
}
