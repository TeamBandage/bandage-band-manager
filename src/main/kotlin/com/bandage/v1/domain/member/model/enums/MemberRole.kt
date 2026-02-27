package com.bandage.v1.domain.member.model.enums

enum class MemberRole(
    val value: String,
) {
    MEMBER("ROLE_MEMBER"),
    ADMIN("ROLE_ADMIN"),
    DEVELOPER("ROLE_DEVELOPER"),
}
