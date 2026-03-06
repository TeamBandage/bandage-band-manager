package com.bandage.v1.domain.auth.dto.req

import com.bandage.v1.global.common.domain.enums.MemberRole

data class MemberAuthCreateRequest(
    val memberId: Long,
    val email: String,
    val password: String,
    val role: MemberRole,
)
