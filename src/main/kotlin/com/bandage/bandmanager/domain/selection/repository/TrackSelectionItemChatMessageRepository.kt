package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionItemChatMessageRepository :
    JpaRepository<TrackSelectionItemChatMessage, UUID>,
    TrackSelectionItemChatMessageRepositoryCustom {
    @Query(
        """
        SELECT c.item.id AS itemId, COUNT(c) AS messageCount
        FROM TrackSelectionItemChatMessage c
        WHERE c.item.id IN :itemIds
        GROUP BY c.item.id
        """,
    )
    fun countByItemIds(
        @Param("itemIds") itemIds: Collection<UUID>,
    ): List<ChatMessageCountRow>
}

interface ChatMessageCountRow {
    fun getItemId(): UUID

    fun getMessageCount(): Long
}
