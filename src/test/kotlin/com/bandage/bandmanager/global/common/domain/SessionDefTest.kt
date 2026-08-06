package com.bandage.bandmanager.global.common.domain

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** sessionId 발급·검증 규칙(BD-269). 약어 생성 규칙은 [SessionAbbreviationGeneratorTest] 참고. */
class SessionDefTest {
    @Test
    fun `sessionId 를 생략하면 서버가 발급한다`() {
        val defs = SessionDef.createAll(listOf(SessionSpec(label = "GUITAR"), SessionSpec(label = "BASS")))

        assertThat(defs.map { it.sessionId }).hasSize(2).doesNotHaveDuplicates().allSatisfy { assertThat(it).isNotBlank() }
    }

    @Test
    fun `기존 sessionId 를 지정하면 그대로 보존한다`() {
        val defs = SessionDef.createAll(listOf(SessionSpec("s-1", "GUITAR")), existingSessionIds = setOf("s-1"))

        assertThat(defs.single().sessionId).isEqualTo("s-1")
    }

    @Test
    fun `기존 세션 유지와 신규 추가를 한 번에 처리한다`() {
        val defs =
            SessionDef.createAll(
                listOf(SessionSpec("s-1", "GUITAR"), SessionSpec(label = "BASS")),
                existingSessionIds = setOf("s-1"),
            )

        assertThat(defs[0].sessionId).isEqualTo("s-1")
        assertThat(defs[1].sessionId).isNotBlank().isNotEqualTo("s-1")
    }

    @Test
    fun `존재하지 않는 sessionId 를 지정하면 예외가 발생한다`() {
        // 기존에는 배정이 조용히 삭제됐다 — 이제 명시적으로 거절한다.
        assertThatThrownBy { SessionDef.createAll(listOf(SessionSpec("unknown", "GUITAR")), setOf("s-1")) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND)
    }

    @Test
    fun `같은 sessionId 를 중복 지정하면 예외가 발생한다`() {
        assertThatThrownBy {
            SessionDef.createAll(listOf(SessionSpec("s-1", "GUITAR"), SessionSpec("s-1", "BASS")), setOf("s-1"))
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_DUPLICATED)
    }

    @Test
    fun `세션을 제거한 목록은 남은 sessionId 만 유지한다`() {
        val defs = SessionDef.createAll(listOf(SessionSpec("s-2", "BASS")), existingSessionIds = setOf("s-1", "s-2"))

        assertThat(defs.map { it.sessionId }).containsExactly("s-2")
    }
}
