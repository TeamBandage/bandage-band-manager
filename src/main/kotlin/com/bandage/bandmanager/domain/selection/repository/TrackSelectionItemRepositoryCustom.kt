package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItem
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface TrackSelectionItemRepositoryCustom {
    /**
     * 선곡 항목 커서 목록 조회. [filter] 조건은 모두 WHERE 로 내려가므로 커서/hasNext 가 정확하다.
     *
     * @param memberId 인증된 요청자(필터 f2 판정용). 필터 입력이 아니라 신원이므로 [filter] 와 분리한다.
     */
    fun findAllBySelectionAndPaging(
        selectionId: UUID,
        memberId: Long,
        filter: TrackSelectionItemFilter,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<TrackSelectionItem, UUID>
}
