package com.bandage.v1.domain.auth.model.enums

enum class MemberRole(
    val value: String,
) {
    MEMBER("ROLE_MEMBER"),
    ADMIN("ROLE_ADMIN"),
    DEVELOPER("ROLE_DEVELOPER"),
}
