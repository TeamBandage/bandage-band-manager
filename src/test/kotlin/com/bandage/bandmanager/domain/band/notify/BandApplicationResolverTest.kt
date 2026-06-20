package com.bandage.bandmanager.domain.band.notify

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandMember
import com.bandage.bandmanager.domain.band.model.enums.BandRole
import com.bandage.bandmanager.domain.band.repository.BandMemberRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class BandApplicationResolverTest {
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val sut = BandApplicationResolver(bandMemberRepository)

    private val bandId: UUID = UUID.randomUUID()

    private fun leader(memberId: Long): BandMember {
        val band = Band.create("밴드", "설명", null)
        return BandMember.create(band, memberId, BandRole.LEADER)
    }

    @Test
    fun `밴드 리더들에게 가입신청 알림 페이로드를 생성한다`() {
        `when`(bandMemberRepository.findAllByBandIdAndRole(bandId, BandRole.LEADER))
            .thenReturn(listOf(leader(10L), leader(20L)))

        val result = sut.resolve(arrayOf<Any?>(bandId, 5L), null)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.recipientId }).containsExactlyInAnyOrder(10L, 20L)
        assertThat(result).allSatisfy {
            assertThat(it.category).isEqualTo(NotifyCategory.BAND_APPLICATION)
            assertThat(it.referenceId).isEqualTo(bandId.toString())
        }
    }

    @Test
    fun `리더가 없으면 빈 리스트를 반환한다`() {
        `when`(bandMemberRepository.findAllByBandIdAndRole(bandId, BandRole.LEADER)).thenReturn(emptyList())

        val result = sut.resolve(arrayOf<Any?>(bandId, 5L), null)

        assertThat(result).isEmpty()
    }
}
