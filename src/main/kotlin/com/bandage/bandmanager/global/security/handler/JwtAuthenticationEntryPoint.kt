package com.bandage.bandmanager.global.security.handler

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val errorCode = ErrorCode.UNAUTHORIZED
        response.status = errorCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, """Bearer error="invalid_token"""")
        response.writer.write(
            """{"success":false,"message":"${errorCode.message}","code":"${errorCode.name}","data":null,"timestamp":"${LocalDateTime.now()}"}""",
        )
    }
}
