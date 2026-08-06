package com.bandage.bandmanager.domain.performance.service

import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.domain.performance.dto.req.PerformancePosterPagingQuery
import com.bandage.bandmanager.domain.performance.model.Performance
import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import com.bandage.bandmanager.domain.performance.repository.PerformanceManagerRepository
import com.bandage.bandmanager.domain.performance.repository.PerformancePosterRepository
import com.bandage.bandmanager.domain.performance.repository.PerformanceRepository
import com.bandage.bandmanager.global.common.response.CursorResponse
import com.bandage.bandmanager.global.infra.s3.CloudFrontUrlResolver
import com.bandage.bandmanager.global.infra.s3.ImagePresignSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class PerformancePosterServiceTest {
    private val performancePosterRepository = mock(PerformancePosterRepository::class.java)
    private val performanceRepository = mock(PerformanceRepository::class.java)
    private val performanceManagerRepository = mock(PerformanceManagerRepository::class.java)
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val cloudFrontUrlResolver = mock(CloudFrontUrlResolver::class.java)
    private val imagePresignSupport = mock(ImagePresignSupport::class.java)

    private val sut =
        PerformancePosterService(
            performancePosterRepository,
            performanceRepository,
            performanceManagerRepository,
            bandMemberRepository,
            cloudFrontUrlResolver,
            imagePresignSupport,
        )

    @Test
    fun `포스터 목록 조회는 리포지토리의 커서 정보를 그대로 응답에 전달한다`() {
        val nextCursor = UUID.randomUUID()
        val poster = poster()
        `when`(cloudFrontUrlResolver.resolve(anyString())).thenReturn("https://cdn.test/poster.png")
        `when`(performancePosterRepository.findAllByPaging(null, null, 10))
            .thenReturn(CursorResponse(listOf(poster), nextCursor, true))

        val result = sut.getPosters(null, PerformancePosterPagingQuery())

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].imageUrl).isEqualTo("https://cdn.test/poster.png")
        assertThat(result.nextCursor).isEqualTo(nextCursor)
        assertThat(result.hasNext).isTrue()
    }

    @Test
    fun `내 포스터 목록 조회는 소속 밴드 ID를 조회해 커서 쿼리에 전달한다`() {
        val memberId = 1L
        val bandIds = listOf(UUID.randomUUID())
        val lastId = UUID.randomUUID()
        `when`(bandMemberRepository.findAllBandIdsByMember(memberId)).thenReturn(bandIds)
        `when`(performancePosterRepository.findMyPostersByCursor(memberId, bandIds, lastId, 5))
            .thenReturn(CursorResponse(emptyList(), null, false))

        val result = sut.getMyPosters(memberId, PerformancePosterPagingQuery(lastId = lastId, pageSize = 5))

        assertThat(result.content).isEmpty()
        assertThat(result.nextCursor).isNull()
        assertThat(result.hasNext).isFalse()
    }

    private fun poster(): PerformancePoster {
        val performance =
            Performance.create(
                title = "테스트 공연",
                startAt = LocalDateTime.of(2026, 7, 1, 19, 0),
                durationMinutes = 90,
                venue = "Club FF",
            )
        setEntityId(performance, UUID.randomUUID())
        val poster = PerformancePoster.create(performance = performance, imageKey = "poster/test.png", description = null)
        setEntityId(poster, UUID.randomUUID())
        return poster
    }

    private fun setEntityId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }
}
