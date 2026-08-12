package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.config.querydsl.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

/**
 * 항목별 채팅 카운트(BD-284) 집계 검증.
 *
 * 페이징 쿼리와 분리된 집계라는 것이 요구사항의 핵심이므로,
 * pageSize 보다 많은 메시지를 넣고 카운트가 limit 에 영향받지 않는지 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class TrackSelectionItemChatMessageCountTest {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var sut: TrackSelectionItemChatMessageRepository

    private lateinit var selection: TrackSelection

    @BeforeEach
    fun setUp() {
        selection = persist(TrackSelection.create(title = "선곡 회의", managerId = 1L))
    }

    @Test
    fun `카운트는 페이징 limit 과 무관하게 전체 메시지 수를 반환한다`() {
        val item = item("Song A")
        repeat(25) { message(item, "msg$it") }

        val page = sut.findAllByItemAndPaging(item.id, lastId = null, pageSize = 10)
        assertThat(page.content).hasSize(10)
        assertThat(page.hasNext).isTrue()

        assertThat(counts(item.id)[item.id]).isEqualTo(25L)
    }

    @Test
    fun `항목별로 각각의 메시지 수를 집계하고 메시지가 없는 항목은 결과에 없다`() {
        val a = item("Song A")
        val b = item("Song B")
        val empty = item("Song C")
        repeat(3) { message(a, "a$it") }
        repeat(1) { message(b, "b$it") }

        val counts = counts(a.id, b.id, empty.id)

        assertThat(counts[a.id]).isEqualTo(3L)
        assertThat(counts[b.id]).isEqualTo(1L)
        assertThat(counts).doesNotContainKey(empty.id)
    }

    @Test
    fun `soft delete 된 메시지는 카운트에서 제외된다`() {
        val item = item("Song A")
        val messages = (1..4).map { message(item, "msg$it") }
        messages.first().markAsDeleted(1L)
        em.flush()
        em.clear()

        assertThat(counts(item.id)[item.id]).isEqualTo(3L)
    }

    // ---------- fixtures ----------

    private fun counts(vararg itemIds: UUID): Map<UUID, Long> =
        sut.countByItemIds(itemIds.toList()).associate { it.getItemId() to it.getMessageCount() }

    private fun item(title: String): TrackSelectionItem =
        persist(
            TrackSelectionItem.create(
                selection = selection,
                trackInfo = TrackInfo(title = title, artist = "Artist", album = null),
                proposerId = 1L,
                note = null,
                sessions = emptyList(),
            ),
        )

    private fun message(
        item: TrackSelectionItem,
        message: String,
    ): TrackSelectionItemChatMessage = persist(TrackSelectionItemChatMessage.create(item, memberId = 1L, message = message))

    private fun <T : Any> persist(entity: T): T {
        em.persist(entity)
        em.flush()
        return entity
    }
}
