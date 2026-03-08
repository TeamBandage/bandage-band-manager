package com.bandage.v1.global.security.constants

import com.bandage.v1.global.common.constants.PathPrefix

object SecurityPathConstants {
    val AUTH_WHITELIST =
        arrayOf(
            "${PathPrefix.PREFIX}/auth/login",
            "${PathPrefix.PREFIX}/members/join",
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
//            "${PathPrefix.PREFIX}/bands/**",
            "${PathPrefix.PREFIX}/practices/**",
            "${PathPrefix.PREFIX}/performances/**",
        )
}
