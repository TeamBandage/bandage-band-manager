package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistItem
import com.bandage.v1.domain.setlist.model.SetlistItemConfirmation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistItemConfirmationRepository : JpaRepository<SetlistItemConfirmation, UUID> {
    fun findAllByItem(item: SetlistItem): List<SetlistItemConfirmation>

    fun findAllByItemIn(items: List<SetlistItem>): List<SetlistItemConfirmation>

    fun findByItemAndSessionIdAndMemberId(
        item: SetlistItem,
        sessionId: String,
        memberId: Long,
    ): SetlistItemConfirmation?

    fun countByItemAndSessionId(
        item: SetlistItem,
        sessionId: String,
    ): Long

    fun deleteAllByItemInAndMemberId(
        items: List<SetlistItem>,
        memberId: Long,
    )
}
