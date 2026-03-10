package com.bandage.v1.global.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "커서 기반 페이징 응답")
data class CursorResponse<T, ID>(
    val content: List<T>,
    val nextCursor: ID?,
    val hasNext: Boolean,
)
