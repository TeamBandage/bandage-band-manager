package com.bandage.bandmanager.domain.auth.dto.req

data class MemberAuthCreateRequest(
    val memberId: Long,
    val email: String,
    val rawPassword: String,
)
