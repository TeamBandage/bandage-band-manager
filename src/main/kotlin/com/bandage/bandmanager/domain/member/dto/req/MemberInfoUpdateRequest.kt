package com.bandage.bandmanager.domain.member.dto.req

import jakarta.annotation.Nullable

data class MemberInfoUpdateRequest(
    @Nullable val name: String?,
    @Nullable val contact: String?,
    @Nullable val profileImg: String?,
)
