package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistItem
import com.bandage.v1.domain.setlist.model.SetlistItemApplicant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistItemApplicantRepository : JpaRepository<SetlistItemApplicant, UUID> {
    fun findAllByItem(item: SetlistItem): List<SetlistItemApplicant>

    fun findAllByItemIn(items: List<SetlistItem>): List<SetlistItemApplicant>

    fun findByItemAndSessionIdAndMemberId(
        item: SetlistItem,
        sessionId: String,
        memberId: Long,
    ): SetlistItemApplicant?

    fun existsByItemAndSessionIdAndMemberId(
        item: SetlistItem,
        sessionId: String,
        memberId: Long,
    ): Boolean

    fun deleteAllByItemInAndMemberId(
        items: List<SetlistItem>,
        memberId: Long,
    )
}
