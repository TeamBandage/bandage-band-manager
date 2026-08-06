package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface PerformancePosterRepositoryCustom {
    /** performanceId 미지정 시 전체 포스터를 커서 기반으로 조회한다. */
    fun findAllByPaging(
        performanceId: UUID?,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformancePoster, UUID>

    /** 회원이 참여(OWNER/MANAGER 또는 소속 밴드 셋리스트)하는 공연의 포스터를 커서 기반으로 조회한다. */
    fun findMyPostersByCursor(
        memberId: Long,
        bandIds: List<UUID>,
        lastId: UUID?,
        pageSize: Int,
    ): CursorResponse<PerformancePoster, UUID>
}
