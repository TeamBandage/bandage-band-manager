package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionRepository
import com.bandage.bandmanager.domain.setlist.dto.req.SetlistCreateRequest
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistResponse
import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.model.SetlistBand
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.domain.setlist.repository.SetlistBandRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import com.bandage.bandmanager.global.notify.annotation.Notify
import com.bandage.bandmanager.global.notify.annotation.NotifyCategory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SetlistCreateFacade(
    private val trackSelectionRepository: TrackSelectionRepository,
    private val trackSelectionBandRepository: TrackSelectionBandRepository,
    private val trackSelectionItemRepository: TrackSelectionItemRepository,
    private val confirmationRepository: TrackSelectionItemConfirmationRepository,
    private val setlistRepository: SetlistRepository,
    private val setlistBandRepository: SetlistBandRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
) {
    @Notify(NotifyCategory.SETLIST_CREATED)
    @Transactional
    fun createSetlist(
        memberId: Long,
        request: SetlistCreateRequest,
    ): SetlistResponse {
        val selection =
            trackSelectionRepository.findByIdOrNull(request.trackSelectionId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)
        if (selection.managerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_MANAGER)
        }
        if (!selection.isLocked) {
            throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_LOCKED)
        }

        val selectedItems = trackSelectionItemRepository.findAllBySelectionAndIsSelectedTrue(selection)
        if (selectedItems.isEmpty()) {
            throw BusinessException(ErrorCode.SETLIST_NO_SELECTED_TRACK)
        }

        val setlist =
            setlistRepository.save(
                Setlist.create(
                    trackSelectionId = selection.id,
                    title = request.title ?: selection.title,
                    managerId = memberId,
                ),
            )

        trackSelectionBandRepository.findAllBySelection(selection).forEach { band ->
            setlistBandRepository.save(SetlistBand.create(bandId = band.bandId, setlistId = setlist.id))
        }

        selectedItems.forEach { item ->
            val track =
                setlistTrackRepository.save(
                    SetlistTrack.create(
                        setlist = setlist,
                        trackInfo =
                            TrackInfo(
                                title = item.trackInfo.title,
                                artist = item.trackInfo.artist,
                                album = item.trackInfo.album,
                                duration = item.trackInfo.duration,
                                reference = item.trackInfo.reference,
                            ),
                        note = item.note,
                        // SessionDef 새 인스턴스로 깊은 복사(두 엔티티의 ElementCollection 간 인스턴스 공유 방지).
                        // 약어는 선곡 항목에서 이미 목록 단위로 생성된 값이라 그대로 옮긴다.
                        sessions = item.sessions.map { SessionDef(it.sessionId, it.label, it.short, it.custom) },
                    ),
                )
            confirmationRepository.findAllByItem(item).forEach { conf ->
                setlistTrackParticipantRepository.save(
                    SetlistTrackParticipant.create(
                        track = track,
                        sessionId = conf.sessionId,
                        memberId = conf.memberId,
                    ),
                )
            }
        }

        return SetlistResponse.of(setlist)
    }
}
