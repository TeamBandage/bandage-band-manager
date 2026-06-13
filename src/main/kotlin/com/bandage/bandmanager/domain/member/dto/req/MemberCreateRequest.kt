package com.bandage.bandmanager.domain.member.dto.req

data class MemberCreateRequest(
    val email: String,
    val name: String,
    val contact: String,
)
