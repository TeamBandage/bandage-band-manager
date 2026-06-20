package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.Setlist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistRepository :
    JpaRepository<Setlist, UUID>,
    SetlistRepositoryCustom {
    fun findAllByTrackSelectionId(trackSelectionId: UUID): List<Setlist>

    /** 회원이 매니저인 (삭제되지 않은) 셋리스트를 일괄 조회(탈퇴 시 권한 양도용). */
    fun findAllByManagerId(managerId: Long): List<Setlist>
}
