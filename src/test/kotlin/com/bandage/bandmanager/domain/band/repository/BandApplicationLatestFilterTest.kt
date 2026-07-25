package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.global.config.querydsl.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 매니저 밴드 신청서 목록의 최신 신청서 필터(BD-227) 검증.
 *
 * QueryDSL 술어라 mock 단위 테스트로는 검증할 수 없어 슬라이스 테스트로 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class BandApplicationLatestFilterTest {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var sut: BandApplicationRepository

    private lateinit var band: Band

    @BeforeEach
    fun setUp() {
        band = Band.create(name = "밴드", description = "설명", profileImg = null)
        em.persist(band)
        em.flush()
    }

    @Test
    fun `과거 이력은 매니저 목록에서 제외되고 최신 신청서만 조회된다`() {
        // 같은 회원이 재신청 → 과거 건은 isLatest=false 로 전환된다
        val outdated = application(memberId = 1L, status = ApplicationStatus.PENDING, latest = false)
        val latest = application(memberId = 1L, status = ApplicationStatus.PENDING, latest = true)

        val result = sut.findAllByPaging(null, 50, ApplicationStatus.PENDING, band)

        assertThat(result.content.map { it.id }).containsExactly(latest.id)
        assertThat(result.content.map { it.id }).doesNotContain(outdated.id)
    }

    @Test
    fun `회원별로 최신 신청서 한 건씩만 조회된다`() {
        application(memberId = 1L, status = ApplicationStatus.PENDING, latest = false)
        val latestOfOne = application(memberId = 1L, status = ApplicationStatus.PENDING, latest = true)
        val latestOfTwo = application(memberId = 2L, status = ApplicationStatus.PENDING, latest = true)

        val result = sut.findAllByPaging(null, 50, ApplicationStatus.PENDING, band)

        assertThat(result.content.map { it.id }).containsExactlyInAnyOrder(latestOfOne.id, latestOfTwo.id)
    }

    @Test
    fun `상태 필터는 최신 신청서 안에서만 적용된다`() {
        // 최신은 REJECTED, 과거 PENDING 이력이 남아 있는 회원
        application(memberId = 1L, status = ApplicationStatus.PENDING, latest = false)
        val rejected = application(memberId = 1L, status = ApplicationStatus.REJECTED, latest = true)

        assertThat(sut.findAllByPaging(null, 50, ApplicationStatus.PENDING, band).content).isEmpty()
        assertThat(sut.findAllByPaging(null, 50, ApplicationStatus.REJECTED, band).content.map { it.id })
            .containsExactly(rejected.id)
    }

    private fun application(
        memberId: Long,
        status: ApplicationStatus,
        latest: Boolean,
    ): BandApplication {
        val application = BandApplication.create(band, memberId)
        if (status != ApplicationStatus.PENDING) application.updateStatus(status)
        if (!latest) application.markAsOutdated()
        em.persist(application)
        em.flush()
        return application
    }
}
