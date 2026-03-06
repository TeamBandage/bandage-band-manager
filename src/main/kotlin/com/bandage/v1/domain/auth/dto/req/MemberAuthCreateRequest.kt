package com.bandage.v1.domain.auth.dto.req

data class MemberAuthCreateRequest(
    val memberId: Long,
    val email: String,
    val rawPassword: String,
)
