package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.performance.service.PerformanceService
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistParticipantResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistTrackResponse
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.service.SetlistService
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * BD-264: 공연에 묶인 셋리스트끼리는 서로의 트랙을 조회할 수 있어야 한다.
 *
 * 시나리오 — 공연 OWNER A 가 매니저 B 를 초대하고, B 가 자기 셋리스트를 공연에 추가한 경우
 * A 는 B 의 셋리스트 트랙까지 볼 수 있어야 한다(셋리스트 단위 권한으로는 막히는 케이스).
 */
class PerformanceSetlistTrackFacadeTest {
    private val performanceService = mock(PerformanceService::class.java)
    private val setlistService = mock(SetlistService::class.java)
    private val setlistRepository = mock(SetlistRepository::class.java)

    private val sut = PerformanceSetlistTrackFacade(performanceService, setlistService, setlistRepository)

    private val performanceId = UUID.randomUUID()
    private val memberA = 1L
    private val memberB = 2L

    @Test
    fun `공연 참여자는 본인이 소유하지 않은 셋리스트의 트랙과 참여자도 조회한다`() {
        val setlistA = setlist(title = "A 셋리스트", managerId = memberA)
        val setlistB = setlist(title = "B 셋리스트", managerId = memberB)
        val trackOfB = trackResponse(setlistB.id, "B 의 곡")

        `when`(performanceService.getAccessibleSetlistIds(performanceId, memberA))
            .thenReturn(listOf(setlistA.id, setlistB.id))
        `when`(setlistRepository.findAllById(listOf(setlistA.id, setlistB.id)))
            .thenReturn(listOf(setlistA, setlistB))
        `when`(setlistService.getTracksBySetlistIds(anyCollection()))
            .thenReturn(mapOf(setlistB.id to listOf(trackOfB)))
        `when`(setlistService.getParticipantsBySetlistIds(anyCollection()))
            .thenReturn(mapOf(setlistB.id to listOf(participantResponse(isManager = true))))

        val result = sut.getSetlistTracks(performanceId, memberA)

        // A 소유가 아닌 setlistB 의 트랙·참여자가 응답에 포함된다 — 이것이 BD-264 의 핵심.
        val bResult = result.single { it.setlist.setlistId == setlistB.id }
        assertThat(bResult.setlist.managerId).isEqualTo(memberB)
        assertThat(bResult.tracks).extracting("title").containsExactly("B 의 곡")
        assertThat(bResult.participants).hasSize(1)

        // 트랙·참여자가 없는 셋리스트도 빈 목록으로 함께 반환된다(누락 아님).
        val aResult = result.single { it.setlist.setlistId == setlistA.id }
        assertThat(aResult.tracks).isEmpty()
        assertThat(aResult.participants).isEmpty()
    }

    @Test
    fun `공연 참여자가 아니면 조회할 수 없다`() {
        val outsider = 99L
        `when`(performanceService.getAccessibleSetlistIds(performanceId, outsider))
            .thenThrow(BusinessException(ErrorCode.NOT_A_PERFORMANCE_MANAGER))

        assertThrows<BusinessException> { sut.getSetlistTracks(performanceId, outsider) }

        // 권한 검증 전에 트랙을 조립하지 않는다.
        verify(setlistService, never()).getTracksBySetlistIds(anyCollection())
        verify(setlistService, never()).getParticipantsBySetlistIds(anyCollection())
    }

    @Test
    fun `공연에 묶인 셋리스트가 없으면 빈 목록을 반환한다`() {
        `when`(performanceService.getAccessibleSetlistIds(performanceId, memberA)).thenReturn(emptyList())

        assertThat(sut.getSetlistTracks(performanceId, memberA)).isEmpty()
        verify(setlistService, never()).getTracksBySetlistIds(anyCollection())
        verify(setlistService, never()).getParticipantsBySetlistIds(anyCollection())
    }

    private fun setlist(
        title: String,
        managerId: Long,
    ): Setlist {
        val setlist = Setlist.create(trackSelectionId = UUID.randomUUID(), title = title, managerId = managerId)
        // id 는 영속화 시점에 생성되므로 테스트에서는 리플렉션으로 주입한다.
        Setlist::class.java
            .getDeclaredField("id")
            .apply { isAccessible = true }
            .set(setlist, UUID.randomUUID())
        return setlist
    }

    private fun participantResponse(isManager: Boolean) =
        SetlistParticipantResponse(member = null, isManager = isManager, sessions = emptyList())

    private fun trackResponse(
        setlistId: UUID,
        title: String,
    ) = SetlistTrackResponse(
        setlistTrackId = UUID.randomUUID(),
        setlistId = setlistId,
        title = title,
        artist = "아티스트",
        album = null,
        duration = null,
        reference = null,
        note = null,
        sessions = emptyList(),
        createdAt = null,
        updatedAt = null,
    )
}
