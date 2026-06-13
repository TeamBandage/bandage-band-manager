package com.bandage.bandmanager.global

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 커밋된 docs/openapi.json 이 현재 코드가 생성하는 OpenAPI 스펙과 일치하는지 검증한다(검증 모드).
 *
 * 목적: 작업 브랜치에서 API(Controller/DTO/어노테이션)를 바꾸고 docs/openapi.json 갱신을 누락하면
 *       CI(`./gradlew build`)가 실패하도록 하여, BE/FE 영향평가 Tool 의 입력(스펙)을 코드와 강제 동기화한다.
 *
 * 비교 시 환경마다 달라지는 `servers` 필드는 정규화로 제외한다(나머지 paths/components/info/tags 는 코드 결정적).
 * 불일치 시: ./gradlew dumpOpenApiSpec 또는 본 테스트가 남긴 build/openapi/generated-openapi.json 으로 갱신할 것.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test", "swagger")
class OpenApiSpecGenerationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @Test
    fun `committed docs-openapi-json matches spec generated from current code`() {
        // 현재 코드 기준 스펙 생성 (springdoc api-docs 엔드포인트)
        val generatedJson =
            mockMvc
                .get("/api-docs")
                .andReturn()
                .response
                .getContentAsString(StandardCharsets.UTF_8)
        val generated = normalize(objectMapper.readTree(generatedJson))

        // 디버깅/갱신 보조용으로 생성 결과를 항상 남긴다
        val dump = File("build/openapi/generated-openapi.json")
        dump.parentFile.mkdirs()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dump, generated)

        // 커밋된 스펙 로드
        val committedFile = File("docs/openapi.json")
        assertTrue(committedFile.exists()) {
            "docs/openapi.json 이 없습니다. `./gradlew dumpOpenApiSpec` 로 생성 후 커밋하세요."
        }
        val committed = normalize(objectMapper.readTree(committedFile))

        assertTrue(generated == committed) {
            buildString {
                appendLine("docs/openapi.json 이 현재 코드가 생성하는 스펙과 다릅니다.")
                appendLine("API 를 변경했다면 스펙 갱신을 같은 PR 에 포함하세요:")
                appendLine("  1) ./gradlew bootRun  (로컬 앱 기동)")
                appendLine("  2) ./gradlew dumpOpenApiSpec")
                appendLine("생성된 스펙은 build/openapi/generated-openapi.json 에서 확인할 수 있습니다.")
            }
        }
    }

    /** 환경 의존 필드(servers)를 제거해 비교를 안정화한다. */
    private fun normalize(node: JsonNode): JsonNode {
        if (node is ObjectNode) {
            node.remove("servers")
        }
        return node
    }
}
