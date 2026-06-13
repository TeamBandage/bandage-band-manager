package com.bandage.bandmanager.domain.auth.oauth

import com.bandage.bandmanager.domain.auth.model.enums.ProviderType

data class OAuthUserInfo(
    val provider: ProviderType,
    val providerId: String,
    val email: String,
    val name: String,
    val profileImg: String? = null,
)
