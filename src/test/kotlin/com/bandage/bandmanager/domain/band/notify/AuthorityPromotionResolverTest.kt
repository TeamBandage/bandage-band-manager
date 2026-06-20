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
import java.util.Optional
import java.util.UUID

class AuthorityPromotionResolverTest {
    private val bandMemberRepository = mock(BandMemberRepository::class.java)
    private val sut = AuthorityPromotionResolver(bandMemberRepository)

    @Test
    fun `새 리더에게 승격 알림을 생성한다`() {
        val bandId = UUID.randomUUID()
        val bandMemberId = UUID.randomUUID()
        val band = Band.create("밴드", "설명", null)
        val newLeader = BandMember.create(band, 42L, BandRole.LEADER)
        setId(newLeader, bandMemberId)
        `when`(bandMemberRepository.findById(bandMemberId)).thenReturn(Optional.of(newLeader))

        val result = sut.resolve(arrayOf<Any?>(bandId, bandMemberId, 1L), null)

        assertThat(result).hasSize(1)
        assertThat(result[0].recipientId).isEqualTo(42L)
        assertThat(result[0].category).isEqualTo(NotifyCategory.AUTHORITY_PROMOTION)
        assertThat(result[0].referenceId).isEqualTo(bandId.toString())
    }

    @Test
    fun `대상 밴드멤버가 없으면 빈 리스트를 반환한다`() {
        val bandId = UUID.randomUUID()
        val bandMemberId = UUID.randomUUID()
        `when`(bandMemberRepository.findById(bandMemberId)).thenReturn(Optional.empty())

        val result = sut.resolve(arrayOf<Any?>(bandId, bandMemberId, 1L), null)

        assertThat(result).isEmpty()
    }

    private fun setId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }
}
