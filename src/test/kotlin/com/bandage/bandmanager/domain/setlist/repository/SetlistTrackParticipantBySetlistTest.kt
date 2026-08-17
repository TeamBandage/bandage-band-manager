package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.config.querydsl.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

/**
 * 자동배치가 쓰는 셋리스트 단위 참여자 조회(BD-272) 검증.
 *
 * SetlistTrack 을 먼저 로드하지 않고 한 번에 가져오는 것이 목적이므로,
 * 다른 셋리스트가 섞이지 않는지와 track fetch join 이 동작하는지를 본다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class SetlistTrackParticipantBySetlistTest {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var sut: SetlistTrackParticipantRepository

    @Test
    fun `셋리스트에 속한 모든 트랙의 참여자를 한 번에 조회한다`() {
        val setlist = setlist("공연 셋리스트")
        val trackA = track(setlist, "Song A")
        val trackB = track(setlist, "Song B")
        participant(trackA, "vocal", 1L)
        participant(trackA, "guitar", 2L)
        participant(trackB, "drum", 3L)

        val found = sut.findAllBySetlistId(setlist.id)

        assertThat(found).hasSize(3)
        assertThat(found.map { it.memberId }).containsExactlyInAnyOrder(1L, 2L, 3L)
        // fetch join 으로 track 이 함께 로드되어 추가 쿼리 없이 접근된다
        assertThat(found.map { it.track.id }).containsExactlyInAnyOrder(trackA.id, trackA.id, trackB.id)
    }

    @Test
    fun `다른 셋리스트의 참여자는 섞이지 않는다`() {
        val mine = setlist("내 셋리스트")
        val other = setlist("남의 셋리스트")
        participant(track(mine, "Song A"), "vocal", 1L)
        participant(track(other, "Song B"), "vocal", 99L)

        assertThat(sut.findAllBySetlistId(mine.id).map { it.memberId }).containsExactly(1L)
    }

    @Test
    fun `참여자가 없으면 빈 리스트를 반환한다`() {
        val setlist = setlist("빈 셋리스트")
        track(setlist, "Song A")

        assertThat(sut.findAllBySetlistId(setlist.id)).isEmpty()
    }

    private fun setlist(title: String): Setlist =
        persist(
            Setlist.create(trackSelectionId = UUID.randomUUID(), title = title, managerId = 1L),
        )

    private fun track(
        setlist: Setlist,
        title: String,
    ): SetlistTrack =
        persist(
            SetlistTrack.create(
                setlist = setlist,
                trackInfo = TrackInfo(title = title, artist = "아티스트"),
                note = null,
                sessions = listOf(SessionDef("vocal", "보컬", "Vo", false)),
            ),
        )

    private fun participant(
        track: SetlistTrack,
        sessionId: String,
        memberId: Long,
    ): SetlistTrackParticipant = persist(SetlistTrackParticipant.create(track, sessionId, memberId))

    private fun <T : Any> persist(entity: T): T {
        em.persist(entity)
        em.flush()
        em.clear()
        return entity
    }
}
