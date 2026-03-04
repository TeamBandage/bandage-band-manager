package com.bandage.v1.global.security

import com.bandage.v1.global.common.constants.PathPrefix.PREFIX

object SecurityPathConstants {
    val AUTH_WHITELIST =
        arrayOf(
            "$PREFIX/auth/login",
            "$PREFIX/members/join",
        )

    val METRICS =
        arrayOf(
            "/actuator/**",
        )

    val SWAGGER_PATHS =
        arrayOf(
            "/api-docs", // yaml 설정 경로
            "/api-docs/**", // 관련 상세 경로
            "/v3/api-docs/**", // 기본 경로
            "/swagger-ui/**", // UI 리소스
            "/swagger-ui.html", // 접속 포인트
            "/webjars/**", // 정적 리소스
        )

    val TMP_FOR_TEST =
        arrayOf(
            "$PREFIX/bands/**",
            "$PREFIX/practices/**",
            "$PREFIX/performances/**",
        )
}
