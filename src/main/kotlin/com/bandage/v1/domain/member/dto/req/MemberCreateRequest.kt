package com.bandage.v1.domain.member.dto.req

data class MemberCreateRequest(
    val email: String,
    val name: String,
    val contact: String,
)
