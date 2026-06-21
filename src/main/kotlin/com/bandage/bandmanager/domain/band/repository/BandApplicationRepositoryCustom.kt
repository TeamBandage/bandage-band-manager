package com.bandage.bandmanager.domain.band.repository

import com.bandage.bandmanager.domain.band.model.Band
import com.bandage.bandmanager.domain.band.model.BandApplication
import com.bandage.bandmanager.domain.band.model.enums.ApplicationStatus
import com.bandage.bandmanager.global.common.response.CursorResponse
import java.util.UUID

interface BandApplicationRepositoryCustom {
    fun findAllByPaging(
        lastId: UUID?,
        pageSize: Int,
        status: ApplicationStatus,
        band: Band,
    ): CursorResponse<BandApplication, UUID>

    /** 특정 회원의 가입 신청 목록을 커서 기반 조회한다. status 가 null 이면 전체 상태를 조회한다. */
    fun findAllByMemberPaging(
        lastId: UUID?,
        pageSize: Int,
        status: ApplicationStatus?,
        memberId: Long,
    ): CursorResponse<BandApplication, UUID>
}
