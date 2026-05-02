package com.bandage.v1.domain.setlist.repository

import com.bandage.v1.domain.setlist.model.SetlistItemChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistItemChatMessageRepository :
    JpaRepository<SetlistItemChatMessage, UUID>,
    SetlistItemChatMessageRepositoryCustom
