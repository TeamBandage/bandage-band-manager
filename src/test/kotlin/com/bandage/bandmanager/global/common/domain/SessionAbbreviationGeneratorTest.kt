package com.bandage.bandmanager.global.common.domain

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SessionAbbreviationGeneratorTest {
    private fun shorts(vararg labels: String): List<String> = SessionAbbreviationGenerator.generate(labels.toList())

    // ---------- 약어 생성 규칙 ----------

    @Test
    fun `첫 글자가 같은 다른 이름이 있으면 두 번째 이름은 두 글자를 쓴다`() {
        assertThat(shorts("VOCAL", "VIOLIN")).containsExactly("V", "VI")
    }

    @Test
    fun `같은 이름이 중복되면 자연수 접미로 구분한다`() {
        assertThat(shorts("VOCAL", "VOCAL", "VIOLIN")).containsExactly("V1", "V2", "VI")
    }

    @Test
    fun `첫 글자가 같은 세 번째 이름은 앞선 이름들과 처음 달라지는 자리의 글자를 쓴다`() {
        // VIOLA 는 VIOLIN 과 index 4(N vs A)에서 처음 달라진다 -> VA
        assertThat(shorts("VOCAL", "VOCAL", "VIOLIN", "VIOLA"))
            .containsExactly("V1", "V2", "VI", "VA")
    }

    @Test
    fun `첫 글자가 같은 네 번째 이름도 처음 달라지는 자리의 글자를 쓴다`() {
        // VIOLB 는 VIOLA 와 index 4(A vs B)에서 처음 달라진다 -> VB
        assertThat(shorts("VOCAL", "VOCAL", "VIOLIN", "VIOLA", "VIOLB"))
            .containsExactly("V1", "V2", "VI", "VA", "VB")
    }

    @Test
    fun `첫 글자가 모두 다르면 모두 한 글자 약어를 쓴다`() {
        assertThat(shorts("GUITAR", "BASS", "DRUM", "VOCAL")).containsExactly("G", "B", "D", "V")
    }

    // ---------- 경계 ----------

    @Test
    fun `빈 목록은 빈 결과를 반환한다`() {
        assertThat(SessionAbbreviationGenerator.generate(emptyList())).isEmpty()
    }

    @Test
    fun `한 글자 이름도 약어로 사용할 수 있다`() {
        assertThat(shorts("V")).containsExactly("V")
    }

    @Test
    fun `한 글자 이름이 먼저 와도 뒤 이름은 두 글자 약어를 받는다`() {
        assertThat(shorts("V", "VOCAL")).containsExactly("V", "VO")
    }

    @Test
    fun `이름이 짧아 구분 자리를 찾을 수 없으면 미사용 조합으로 폴백한다`() {
        assertThat(shorts("VOCAL", "VA", "VB")).containsExactly("V", "VA", "VB")
    }

    @Test
    fun `두 글자 약어끼리 중복되면 자연수 접미를 붙인다`() {
        assertThat(shorts("VOCAL", "VOCAL", "VIOLIN", "VIOLIN"))
            .containsExactly("V1", "V2", "VI1", "VI2")
    }

    @Test
    fun `두 글자 규칙과 숫자 접미가 한 목록에 함께 적용된다`() {
        assertThat(shorts("AB", "AB", "AC")).containsExactly("A1", "A2", "AC")
    }

    @Test
    fun `두 글자로도 구분되지 않으면 첫 글자로 폴백하고 숫자로 구분한다`() {
        assertThat(shorts("A", "AA", "AAA")).containsExactly("A1", "AA", "A2")
    }

    @Test
    fun `중복이 열 개를 넘으면 두 자리 숫자 접미를 사용한다`() {
        // 유일성이 3자 길이 제한보다 우선한다
        assertThat(SessionAbbreviationGenerator.generate(List(11) { "VOCAL" }))
            .containsExactly("V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", "V10", "V11")
    }

    @Test
    fun `생성된 약어는 항상 서로 다르다`() {
        val labels = listOf("VOCAL", "VOCAL", "VIOLIN", "VIOLA", "VIOLB", "GUITAR", "GUITAR", "GRAND")

        assertThat(SessionAbbreviationGenerator.generate(labels)).doesNotHaveDuplicates()
    }

    // ---------- label 검증 / 정규화 ----------

    @Test
    fun `label 은 대문자로 정규화된다`() {
        assertThat(SessionAbbreviationGenerator.normalizeLabel("vocal")).isEqualTo("VOCAL")
    }

    @Test
    fun `label 앞뒤 공백은 제거된다`() {
        assertThat(SessionAbbreviationGenerator.normalizeLabel("  Vocal  ")).isEqualTo("VOCAL")
    }

    @Test
    fun `한글 label 은 예외를 던진다`() {
        assertLabelRejected("기타")
    }

    @Test
    fun `숫자가 포함된 label 은 예외를 던진다`() {
        assertLabelRejected("GUITAR2")
    }

    @Test
    fun `공백이 포함된 label 은 예외를 던진다`() {
        assertLabelRejected("MAIN GUITAR")
    }

    @Test
    fun `특수문자가 포함된 label 은 예외를 던진다`() {
        assertLabelRejected("GUITAR-1")
    }

    @Test
    fun `빈 label 은 예외를 던진다`() {
        assertLabelRejected("   ")
    }

    @Test
    fun `대소문자가 섞여도 정규화 후 목록 단위로 약어가 생성된다`() {
        assertThat(SessionAbbreviationGenerator.normalizeAndGenerate(listOf("vocal", "Violin")))
            .containsExactly("VOCAL" to "V", "VIOLIN" to "VI")
    }

    @Test
    fun `createAll 은 sessionId 와 custom 을 유지하고 label 약어를 생성한다`() {
        val defs =
            SessionDef.createAll(
                listOf(
                    SessionSpec(sessionId = "V-1", label = "vocal", custom = false),
                    SessionSpec(sessionId = "V-2", label = "vocal", custom = true),
                    SessionSpec(sessionId = "G-1", label = "guitar", custom = false),
                ),
            )

        assertThat(defs.map { it.sessionId }).containsExactly("V-1", "V-2", "G-1")
        assertThat(defs.map { it.label }).containsExactly("VOCAL", "VOCAL", "GUITAR")
        assertThat(defs.map { it.short }).containsExactly("V1", "V2", "G")
        assertThat(defs.map { it.custom }).containsExactly(false, true, false)
    }

    private fun assertLabelRejected(label: String) {
        assertThatThrownBy { SessionAbbreviationGenerator.normalizeLabel(label) }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_LABEL_NOT_ALPHABETIC)
    }
}
