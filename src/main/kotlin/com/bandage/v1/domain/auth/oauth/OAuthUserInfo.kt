package com.bandage.v1.domain.auth.oauth

import com.bandage.v1.domain.auth.model.enums.ProviderType

data class OAuthUserInfo(
    val provider: ProviderType,
    val providerId: String,
    val email: String,
    val name: String,
    val profileImg: String? = null,
)
