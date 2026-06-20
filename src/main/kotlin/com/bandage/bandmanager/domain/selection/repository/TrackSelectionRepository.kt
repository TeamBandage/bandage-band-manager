package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionRepository :
    JpaRepository<TrackSelection, UUID>,
    TrackSelectionRepositoryCustom {
    /** 회원이 매니저인 (삭제되지 않은) 선곡 회의를 일괄 조회(탈퇴 시 권한 양도용). */
    fun findAllByManagerId(managerId: Long): List<TrackSelection>
}
