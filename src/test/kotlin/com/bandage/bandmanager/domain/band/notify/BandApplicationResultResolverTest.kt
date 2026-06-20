package com.bandage.bandmanager.domain.band.notify

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.domain.band.repository.BandApplicationRepository
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import java.util.UUID

class BandApplicationResultResolverTest {
    private val applicationRepository = mock(BandApplicationRepository::class.java)
    private val sut = BandApplicationResultResolver(applicationRepository)

    private val bandId: UUID = UUID.randomUUID()

    private fun application(applicantId: Long): BandApplication {
        val band = Band.create("밴드", "설명", null)
        setId(band, bandId)
        val application = BandApplication.create(band, applicantId)
        setId(application, UUID.randomUUID())
        return application
    }

    private fun setId(
        target: Any,
        id: UUID,
    ) {
        val field = target.javaClass.getDeclaredField("id")
        field.isAccessible = true
        field.set(target, id)
    }

    @Test
    fun `승인 결과를 신청자에게 알림한다`() {
        val application = application(7L)
        `when`(applicationRepository.findById(application.id)).thenReturn(Optional.of(application))

        val result = sut.resolve(arrayOf<Any?>(bandId, application.id, 1L, ApplicationStatus.APPROVED), null)

        assertThat(result).hasSize(1)
        assertThat(result[0].recipientId).isEqualTo(7L)
        assertThat(result[0].category).isEqualTo(NotifyCategory.BAND_APPLICATION_RESULT)
        assertThat(result[0].referenceId).isEqualTo(bandId.toString())
    }

    @Test
    fun `거절 결과를 신청자에게 알림한다`() {
        val application = application(7L)
        `when`(applicationRepository.findById(application.id)).thenReturn(Optional.of(application))

        val result = sut.resolve(arrayOf<Any?>(bandId, application.id, 1L, ApplicationStatus.REJECTED), null)

        assertThat(result).hasSize(1)
        assertThat(result[0].recipientId).isEqualTo(7L)
    }

    @Test
    fun `승인거절 외 상태는 알림을 생성하지 않는다`() {
        val application = application(7L)
        `when`(applicationRepository.findById(application.id)).thenReturn(Optional.of(application))

        val result = sut.resolve(arrayOf<Any?>(bandId, application.id, 1L, ApplicationStatus.WITHDRAWN), null)

        assertThat(result).isEmpty()
    }
}
