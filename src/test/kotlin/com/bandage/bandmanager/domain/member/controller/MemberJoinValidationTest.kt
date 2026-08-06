package com.bandage.bandmanager.domain.member.controller

import com.bandage.bandmanager.domain.auth.dto.req.MemberPasswordChangeRequest
import com.bandage.bandmanager.facade.dto.MemberJoinRequest
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * 회원가입 요청 DTO 제약 검증(BD-273).
 *
 * 컨트롤러에 `@Valid` 가 없으면 이 제약들이 선언만 되고 동작하지 않아
 * `@bandage.test` 같은 값으로도 계정이 생성되던 문제가 있었다.
 */
class MemberJoinValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @ParameterizedTest
    @ValueSource(strings = ["not-an-email", "@bandage.test", "a b@bandage.test", "member@"])
    fun `잘못된 형식의 이메일은 제약 위반으로 걸러진다`(email: String) {
        val request = MemberJoinRequest(email = email, password = "12345678", name = "홍길동")

        val violations = validator.validate(request)

        assertThat(violations).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("email") }
    }

    @Test
    fun `정상 요청은 제약 위반이 없다`() {
        val request = MemberJoinRequest(email = "member@bandage.test", password = "12345678", name = "홍길동")

        assertThat(validator.validate(request)).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "1234567"])
    fun `8자 미만 비밀번호는 제약 위반으로 걸러진다`(password: String) {
        val request = MemberJoinRequest(email = "member@bandage.test", password = password, name = "홍길동")

        val violations = validator.validate(request)

        assertThat(violations).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("password") }
    }

    @Test
    fun `비밀번호 변경 요청도 8자 이상을 요구한다`() {
        val request = MemberPasswordChangeRequest(originalPassword = "12345678", newPassword = "short")

        val violations = validator.validate(request)

        assertThat(violations).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("newPassword") }
    }
}
