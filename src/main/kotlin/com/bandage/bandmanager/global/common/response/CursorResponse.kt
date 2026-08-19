package com.bandage.bandmanager.global.common.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "커서 기반 페이징 응답")
data class CursorResponse<T, ID>(
    val content: List<T>,
    val nextCursor: ID?,
    val hasNext: Boolean,
) {
    /** 커서/hasNext 를 유지한 채 content 만 응답 DTO 로 변환한다. */
    fun <R> map(transform: (T) -> R): CursorResponse<R, ID> = CursorResponse(content.map(transform), nextCursor, hasNext)

    companion object {
        /**
         * `limit(pageSize + 1)` 로 조회한 행에서 hasNext/nextCursor 를 계산한다.
         * 초과분 1건은 다음 페이지 존재 여부 판별용이므로 응답에서 제외한다.
         */
        fun <T, ID> of(
            rows: List<T>,
            pageSize: Int,
            cursorOf: (T) -> ID,
        ): CursorResponse<T, ID> {
            val hasNext = rows.size > pageSize
            val content = if (hasNext) rows.dropLast(1) else rows
            return CursorResponse(
                content = content,
                nextCursor = if (hasNext) content.lastOrNull()?.let(cursorOf) else null,
                hasNext = hasNext,
            )
        }
    }
}
