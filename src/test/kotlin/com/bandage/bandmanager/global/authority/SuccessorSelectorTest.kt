package com.bandage.bandmanager.global.authority

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SuccessorSelectorTest {
    private data class Candidate(
        val name: String,
        val createdAt: LocalDateTime,
    )

    private val base: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0)

    @Test
    fun `상위 그룹이 비어있지 않으면 하위 그룹보다 우선해 상위 그룹의 최고참을 선택한다`() {
        val upper =
            listOf(
                Candidate("upperNew", base.plusDays(2)),
                Candidate("upperOld", base.plusDays(1)),
            )
        val lower = listOf(Candidate("lowerOldest", base))

        val result = SuccessorSelector.oldestFromHighestTier(listOf(upper, lower)) { it.createdAt }

        assertThat(result?.name).isEqualTo("upperOld")
    }

    @Test
    fun `상위 그룹이 비어있으면 다음 그룹의 최고참을 선택한다`() {
        val lower =
            listOf(
                Candidate("a", base.plusDays(1)),
                Candidate("b", base),
            )

        val result = SuccessorSelector.oldestFromHighestTier(listOf(emptyList(), lower)) { it.createdAt }

        assertThat(result?.name).isEqualTo("b")
    }

    @Test
    fun `모든 그룹이 비어있으면 null 을 반환한다`() {
        val result =
            SuccessorSelector.oldestFromHighestTier<Candidate>(listOf(emptyList(), emptyList())) { it.createdAt }

        assertThat(result).isNull()
    }
}
