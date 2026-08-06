package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.member.model.Member
import com.bandage.bandmanager.domain.selection.model.TrackSelection
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemApplicant
import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemConfirmation
import com.bandage.bandmanager.domain.selection.model.enums.RecruitStatus
import com.bandage.bandmanager.domain.selection.model.enums.TrackSearchField
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.SessionSpec
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
 * 선곡 항목 필터(BD-228) 통합 검증.
 *
 * mock 으로는 절대 잡히지 않는 것들을 검증한다:
 *  - @ElementCollection(_sessions) 상관 서브쿼리가 유효한 JPQL 로 컴파일되는지
 *  - 세션 0개 항목에서 "모든 세션 충족" 이 공허하게 참이 되지 않는지
 *  - @SQLRestriction 이 상관 서브쿼리에도 전파되는지
 *  - 필터 적용 후에도 커서/hasNext 가 정확한지
 *
 * H2(PostgreSQL 모드)로 실행되므로 실제 PostgreSQL 과 100% 동일하지는 않다.
 * containsIgnoreCase 는 양쪽 모두 lower(x) like lower(?) 로 렌더되고 NOT EXISTS 중첩은 표준 SQL 이다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class TrackSelectionItemFilterTest {
    @Autowired
    private lateinit var em: EntityManager

    // 커스텀 프래그먼트는 Spring Data 가 리포지토리 프록시에 합성해주므로 인터페이스로 주입받는다.
    @Autowired
    private lateinit var sut: TrackSelectionItemRepository

    private lateinit var selection: TrackSelection
    private var aliceId: Long = 0
    private var bobId: Long = 0

    @BeforeEach
    fun setUp() {
        selection = persist(TrackSelection.create(title = "선곡 회의", managerId = 1L))
        aliceId = persist(Member.create(email = "alice@test.com", name = "Alice")).id
        bobId = persist(Member.create(email = "bob@test.com", name = "밥철수")).id
    }

    // ---------- f1: 모집 상태 ----------

    @Test
    fun `세션이 없는 항목은 OPEN 으로 조회되고 완료 상태로는 조회되지 않는다`() {
        val item = item(title = "NoSession", sessions = emptyList())

        assertThat(findIds(status = listOf(RecruitStatus.OPEN))).containsExactly(item.id)
        assertThat(findIds(status = listOf(RecruitStatus.APPLY_COMPLETED))).isEmpty()
        assertThat(findIds(status = listOf(RecruitStatus.ASSIGN_COMPLETED))).isEmpty()
    }

    @Test
    fun `지원자가 없는 세션이 하나라도 있으면 OPEN 이다`() {
        val item = item(title = "Partial", sessions = listOf("V" to "VOCAL", "G" to "GUITAR"))
        applicant(item, "V", aliceId)

        assertThat(findIds(status = listOf(RecruitStatus.OPEN))).containsExactly(item.id)
        assertThat(findIds(status = listOf(RecruitStatus.APPLY_COMPLETED))).isEmpty()
    }

    @Test
    fun `모든 세션에 지원자가 있으면 APPLY_COMPLETED 이고 OPEN 이 아니다`() {
        val item = item(title = "Applied", sessions = listOf("V" to "VOCAL", "G" to "GUITAR"))
        applicant(item, "V", aliceId)
        applicant(item, "G", bobId)

        assertThat(findIds(status = listOf(RecruitStatus.APPLY_COMPLETED))).containsExactly(item.id)
        assertThat(findIds(status = listOf(RecruitStatus.OPEN))).isEmpty()
    }

    @Test
    fun `모든 세션이 확정되면 ASSIGN_COMPLETED 이며 APPLY_COMPLETED 조건도 만족한다`() {
        val item = item(title = "Assigned", sessions = listOf("V" to "VOCAL"))
        applicant(item, "V", aliceId)
        confirmation(item, "V", aliceId)

        // 겹침을 의도적으로 허용한다(독립 조건 OR)
        assertThat(findIds(status = listOf(RecruitStatus.ASSIGN_COMPLETED))).containsExactly(item.id)
        assertThat(findIds(status = listOf(RecruitStatus.APPLY_COMPLETED))).containsExactly(item.id)
    }

    @Test
    fun `isSelected 인 항목은 CLOSED 로 조회된다`() {
        val open = item(title = "StillOpen", sessions = listOf("V" to "VOCAL"))
        val closed = item(title = "Closed", sessions = listOf("V" to "VOCAL"), selected = true)

        assertThat(findIds(status = listOf(RecruitStatus.CLOSED))).containsExactly(closed.id)
        assertThat(findIds(status = listOf(RecruitStatus.OPEN))).containsExactlyInAnyOrder(open.id, closed.id)
    }

    @Test
    fun `status 목록은 OR 로 합집합 조회된다`() {
        val open = item(title = "Open", sessions = listOf("V" to "VOCAL"))
        val closed = item(title = "Closed", sessions = listOf("G" to "GUITAR"), selected = true)
        applicant(closed, "G", aliceId)
        confirmation(closed, "G", aliceId)

        assertThat(findIds(status = listOf(RecruitStatus.OPEN, RecruitStatus.CLOSED)))
            .containsExactlyInAnyOrder(open.id, closed.id)
    }

    @Test
    fun `소프트 삭제된 지원자는 모집 상태 판정에서 제외된다`() {
        val item = item(title = "Withdrawn", sessions = listOf("V" to "VOCAL"))
        val applicant = applicant(item, "V", aliceId)
        applicant.markAsDeleted(aliceId)
        em.flush()
        em.clear()

        // 지원자가 논리 삭제되었으므로 다시 OPEN 이어야 한다
        assertThat(findIds(status = listOf(RecruitStatus.OPEN))).containsExactly(item.id)
        assertThat(findIds(status = listOf(RecruitStatus.APPLY_COMPLETED))).isEmpty()
    }

    // ---------- f2: 내가 지원한 항목 ----------

    @Test
    fun `appliedByMe 로 내가 지원한 항목만 또는 지원하지 않은 항목만 조회한다`() {
        val mine = item(title = "Mine", sessions = listOf("V" to "VOCAL"))
        val others = item(title = "Others", sessions = listOf("V" to "VOCAL"))
        applicant(mine, "V", aliceId)
        applicant(others, "V", bobId)

        assertThat(findIds(appliedByMe = true, memberId = aliceId)).containsExactly(mine.id)
        assertThat(findIds(appliedByMe = false, memberId = aliceId)).containsExactly(others.id)
        assertThat(findIds(memberId = aliceId)).containsExactlyInAnyOrder(mine.id, others.id)
    }

    // ---------- f3: 참여자 이름 검색 ----------

    @Test
    fun `memberName 은 대소문자를 무시하고 부분 일치한다`() {
        val withAlice = item(title = "WithAlice", sessions = listOf("V" to "VOCAL"))
        val withBob = item(title = "WithBob", sessions = listOf("V" to "VOCAL"))
        applicant(withAlice, "V", aliceId)
        applicant(withBob, "V", bobId)

        assertThat(findIds(memberName = "ali")).containsExactly(withAlice.id)
        assertThat(findIds(memberName = "ALI")).containsExactly(withAlice.id)
        assertThat(findIds(memberName = "밥철")).containsExactly(withBob.id)
        assertThat(findIds(memberName = "없는이름")).isEmpty()
    }

    @Test
    fun `memberName 이 공백이면 필터가 적용되지 않는다`() {
        val item = item(title = "Any", sessions = emptyList())

        assertThat(findIds(memberName = "   ")).containsExactly(item.id)
    }

    // ---------- f4: 트랙 정보 검색 ----------

    @Test
    fun `searchFields 미지정이면 제목 아티스트 앨범을 모두 OR 검색한다`() {
        val byTitle = item(title = "Stairway", artist = "X", album = "Y")
        val byArtist = item(title = "A", artist = "Stairway Band", album = "B")
        val byAlbum = item(title = "C", artist = "D", album = "The Stairway")
        item(title = "None", artist = "None", album = "None")

        assertThat(findIds(keyword = "stairway"))
            .containsExactlyInAnyOrder(byTitle.id, byArtist.id, byAlbum.id)
    }

    @Test
    fun `searchFields 를 지정하면 해당 필드만 검색한다`() {
        val byTitle = item(title = "Stairway", artist = "X", album = "Y")
        item(title = "A", artist = "Stairway Band", album = "B")

        assertThat(findIds(keyword = "stairway", searchFields = listOf(TrackSearchField.TITLE)))
            .containsExactly(byTitle.id)
    }

    @Test
    fun `앨범이 null 인 항목은 앨범 검색에 매치되지 않는다`() {
        item(title = "NoAlbum", artist = "X", album = null)

        assertThat(findIds(keyword = "x", searchFields = listOf(TrackSearchField.ALBUM))).isEmpty()
    }

    @Test
    fun `keyword 없이 searchFields 만 오면 필터가 적용되지 않는다`() {
        val item = item(title = "Any", sessions = emptyList())

        assertThat(findIds(searchFields = listOf(TrackSearchField.TITLE))).containsExactly(item.id)
    }

    // ---------- 조합 & 커서 ----------

    @Test
    fun `필터를 조합하면 AND 로 좁혀진다`() {
        val target = item(title = "Stairway", sessions = listOf("V" to "VOCAL"))
        val wrongKeyword = item(title = "Other", sessions = listOf("V" to "VOCAL"))
        val wrongApplicant = item(title = "Stairway Two", sessions = listOf("V" to "VOCAL"))
        applicant(target, "V", aliceId)
        applicant(wrongKeyword, "V", aliceId)
        applicant(wrongApplicant, "V", bobId)

        val found =
            findIds(
                status = listOf(RecruitStatus.APPLY_COMPLETED),
                appliedByMe = true,
                memberName = "alice",
                keyword = "Stairway",
                searchFields = listOf(TrackSearchField.TITLE),
                memberId = aliceId,
            )

        assertThat(found).containsExactly(target.id)
    }

    @Test
    fun `필터 적용 후에도 커서와 hasNext 가 정확하다`() {
        // 매칭 5건 + 비매칭 3건을 섞어 저장한다
        val matching = (1..5).map { item(title = "Match$it", sessions = listOf("V" to "VOCAL"), selected = true) }
        repeat(3) { item(title = "Skip$it", sessions = listOf("V" to "VOCAL")) }

        val collected = mutableListOf<UUID>()
        var cursor: UUID? = null
        var hasNext: Boolean
        var pages = 0
        do {
            val page =
                sut.findAllBySelectionAndPaging(
                    selectionId = selection.id,
                    memberId = aliceId,
                    filter = TrackSelectionItemFilter(status = listOf(RecruitStatus.CLOSED)),
                    lastId = cursor,
                    pageSize = 2,
                )
            collected += page.content.map { it.id }
            cursor = page.nextCursor
            hasNext = page.hasNext
            pages++
        } while (hasNext && pages < 10)

        assertThat(hasNext).isFalse()

        // 중복/누락 없이 매칭 5건만, id 오름차순으로 순회된다
        assertThat(collected).doesNotHaveDuplicates()
        assertThat(collected).containsExactlyElementsOf(matching.map { it.id }.sorted())
    }

    // ---------- fixtures ----------

    private fun findIds(
        status: List<RecruitStatus>? = null,
        appliedByMe: Boolean? = null,
        memberName: String? = null,
        keyword: String? = null,
        searchFields: List<TrackSearchField>? = null,
        memberId: Long = 1L,
    ): List<UUID> =
        sut
            .findAllBySelectionAndPaging(
                selectionId = selection.id,
                memberId = memberId,
                filter = TrackSelectionItemFilter(status, appliedByMe, memberName, keyword, searchFields),
                lastId = null,
                pageSize = 100,
            ).content
            .map { it.id }

    private fun item(
        title: String,
        artist: String = "Artist",
        album: String? = "Album",
        sessions: List<Pair<String, String>> = emptyList(),
        selected: Boolean = false,
    ): TrackSelectionItem {
        val item =
            TrackSelectionItem.create(
                selection = selection,
                trackInfo = TrackInfo(title = title, artist = artist, album = album),
                proposerId = 1L,
                note = null,
                sessions = sessionDefs(sessions),
            )
        if (selected) item.select()
        return persist(item)
    }

    /** (sessionId, label) 쌍으로 세션 정의를 만든다. 약어는 목록 단위로 생성된다. */
    private fun sessionDefs(sessions: List<Pair<String, String>>): List<SessionDef> =
        SessionDef.createAll(
            sessions.map { (id, label) -> SessionSpec(sessionId = id, label = label) },
            existingSessionIds = sessions.map { it.first }.toSet(),
        )

    private fun applicant(
        item: TrackSelectionItem,
        sessionId: String,
        memberId: Long,
    ): TrackSelectionItemApplicant = persist(TrackSelectionItemApplicant.create(item, sessionId, memberId))

    private fun confirmation(
        item: TrackSelectionItem,
        sessionId: String,
        memberId: Long,
    ): TrackSelectionItemConfirmation = persist(TrackSelectionItemConfirmation.create(item, sessionId, memberId, 1L))

    private fun <T : Any> persist(entity: T): T {
        em.persist(entity)
        em.flush()
        return entity
    }
}
