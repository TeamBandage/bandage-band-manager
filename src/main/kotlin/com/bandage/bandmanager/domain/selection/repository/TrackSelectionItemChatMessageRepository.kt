package com.bandage.bandmanager.domain.selection.repository

import com.bandage.bandmanager.domain.selection.model.TrackSelectionItemChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TrackSelectionItemChatMessageRepository :
    JpaRepository<TrackSelectionItemChatMessage, UUID>,
    TrackSelectionItemChatMessageRepositoryCustom
