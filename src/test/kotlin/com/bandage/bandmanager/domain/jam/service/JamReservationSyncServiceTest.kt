package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.model.JamParticipant
import com.bandage.bandmanager.domain.jam.model.JamReservation
import com.bandage.bandmanager.domain.jam.repository.JamParticipantRepository
import com.bandage.bandmanager.domain.jam.repository.JamReservationRepository
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class JamReservationSyncServiceTest {
    private val reservationRepository = mock(JamReservationRepository::class.java)
    private val participantRepository = mock(JamParticipantRepository::class.java)
    private val sut = JamReservationSyncService(reservationRepository, participantRepository)

    private val startAt = LocalDateTime.of(2026, 6, 10, 19, 0)

    private fun jamWithId(id: UUID): Jam {
        val jam =
            Jam.create(
                title = "합주",
                trackInfo = TrackInfo(title = "곡", artist = "아티스트"),
                startAt = startAt,
                durationMinutes = 120,
                venue = null,
            )
        val field = Jam::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(jam, id)
        return jam
    }

    @Test
    fun `같은 멤버가 여러 세션에 참여해도 예약은 1건만 생성된다`() {
        val id = UUID.randomUUID()
        val jam = jamWithId(id)
        val saved = mutableListOf<JamReservation>()
        val member1 =
            JamParticipant.create(jam, 1L).apply {
                assignSession("vocal")
                assignSession("guitar")
            }
        val member2 = JamParticipant.create(jam, 2L).apply { assignSession("drum") }
        `when`(participantRepository.findAllByJam(jam)).thenReturn(listOf(member1, member2))
        `when`(reservationRepository.saveAll(anyIterable())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val arg = invocation.getArgument(0) as Iterable<JamReservation>
            saved.addAll(arg)
            saved.toList()
        }

        sut.sync(jam)

        verify(reservationRepository).deleteAllByJamId(id)
        assertThat(saved.map { it.memberId }).containsExactlyInAnyOrder(1L, 2L)
        assertThat(saved).allSatisfy {
            assertThat(it.startAt).isEqualTo(startAt)
            assertThat(it.endAt).isEqualTo(startAt.plusMinutes(120))
        }
    }

    @Test
    fun `soft-delete 된 Jam 은 기존 예약을 삭제만 하고 재생성하지 않는다`() {
        val id = UUID.randomUUID()
        val jam = jamWithId(id)
        jam.markAsDeleted(99L)

        sut.sync(jam)

        verify(reservationRepository).deleteAllByJamId(id)
        verify(reservationRepository, never()).saveAll(anyIterable())
    }
}
