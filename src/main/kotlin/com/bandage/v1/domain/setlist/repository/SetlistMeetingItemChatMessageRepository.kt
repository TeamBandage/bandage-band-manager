package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistMeetingItemChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistMeetingItemChatMessageRepository :
    JpaRepository<SetlistMeetingItemChatMessage, UUID>,
    SetlistMeetingItemChatMessageRepositoryCustom
