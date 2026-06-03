package com.bandage.v1.domain.selection.dto.req

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 회귀: 과거 `isSelected: Boolean` 은 Jackson 이 응답을 "selected" 로 직렬화하면서
 * 요청 역직렬화 키와 어긋나 PATCH .../selection 이 항상 400(HttpMessageNotReadable) 이었다.
 * 필드명을 `selected` 로 통일하여 요청/응답 키가 일치함을 고정한다.
 */
class TrackSelectionItemSelectionRequestTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `selected 키로 역직렬화된다`() {
        val req = mapper.readValue("""{"selected":true}""", TrackSelectionItemSelectionRequest::class.java)
        assertThat(req.selected).isTrue()
    }

    @Test
    fun `직렬화 키도 selected 로 일치한다`() {
        val json = mapper.writeValueAsString(TrackSelectionItemSelectionRequest(selected = false))
        assertThat(json).contains("\"selected\"")
        assertThat(json).doesNotContain("isSelected")
    }
}
