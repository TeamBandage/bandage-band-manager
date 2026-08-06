package com.bandage.bandmanager.global.error.handler

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.UUID

/**
 * 클라이언트 입력 오류가 500 으로 새지 않는지 검증한다(BD-273).
 *
 * 두 예외 모두 전용 핸들러가 없으면 catch-all 로 떨어져
 * `GET /bands/not-a-uuid`, 쿠키 없는 `POST /auth/refresh` 가 500 을 반환했다.
 */
class GlobalExceptionHandlerTest {
    /** 핸들러는 protected 관례를 따르므로, 테스트에서만 서브클래스로 노출한다. */
    private class TestableHandler : GlobalExceptionHandler() {
        fun typeMismatch(e: MethodArgumentTypeMismatchException) = handleMethodArgumentTypeMismatch(e)

        fun missingCookie(e: MissingRequestCookieException) = handleMissingRequestCookie(e)
    }

    private val handler = TestableHandler()

    @Suppress("UNUSED_PARAMETER")
    private fun dummy(bandId: UUID) = Unit

    private fun methodParameter(): MethodParameter = MethodParameter(this::class.java.getDeclaredMethod("dummy", UUID::class.java), 0)

    @Test
    fun `경로변수 타입 불일치는 400 으로 응답한다`() {
        val exception =
            MethodArgumentTypeMismatchException("not-a-uuid", UUID::class.java, "bandId", methodParameter(), null)

        val response = handler.typeMismatch(exception)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.code).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.name)
        assertThat(response.body?.fieldErrors).containsKey("bandId")
    }

    @Test
    fun `필수 쿠키 누락은 401 로 응답한다`() {
        val exception = MissingRequestCookieException("refreshToken", methodParameter())

        val response = handler.missingCookie(exception)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.code).isEqualTo(ErrorCode.UNAUTHORIZED.name)
    }
}
