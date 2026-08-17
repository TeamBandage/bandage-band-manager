package com.bandage.bandmanager.domain.availability.repository

import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.global.config.querydsl.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Session
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 자동배치는 참여 멤버 전원의 가용성을 한 번에 조회한 뒤 컬렉션을 순회한다(BD-272).
 * BatchSize 가 없으면 멤버 수만큼 컬렉션 조회가 따로 나가므로, 실제 쿼리 수로 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class MemberAvailabilityBatchFetchTest {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var sut: MemberAvailabilityRepository

    private val mon: LocalDate = LocalDate.of(2026, 6, 1)

    @Test
    fun `멤버 여러 명의 주간 규칙을 배치로 묶어 조회한다`() {
        val memberIds = (1L..10L).toList()
        memberIds.forEach { id ->
            em.persist(
                MemberAvailability.create(id).apply {
                    updateWeeklyRules(
                        listOf(WeeklyRule(DayOfWeek.MONDAY, startSlot = 36, endSlot = 44, effectiveFrom = mon)),
                    )
                },
            )
        }
        em.flush()
        em.clear()

        val statistics = em.unwrap(Session::class.java).sessionFactory.statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val found = sut.findAllByMemberIdIn(memberIds)
        // 컬렉션을 실제로 건드려야 LAZY 로딩이 일어난다
        val totalRules = found.sumOf { it.weeklyRules.size }

        assertThat(found).hasSize(10)
        assertThat(totalRules).isEqualTo(10)
        // 루트 조회 1 + 배치로 묶인 컬렉션 조회 1 = 2. BatchSize 가 없으면 11 이 된다.
        assertThat(statistics.prepareStatementCount).isLessThanOrEqualTo(3)
    }
}
