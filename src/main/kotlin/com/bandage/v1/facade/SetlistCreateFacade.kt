package com.bandage.v1.facade

import com.bandage.v1.domain.selection.repository.TrackSelectionBandRepository
import com.bandage.v1.domain.selection.repository.TrackSelectionItemConfirmationRepository
import com.bandage.v1.domain.selection.repository.TrackSelectionItemRepository
import com.bandage.v1.domain.selection.repository.TrackSelectionRepository
import com.bandage.v1.domain.setlist.dto.req.SetlistCreateRequest
import com.bandage.v1.domain.setlist.dto.res.SetlistResponse
import com.bandage.v1.domain.setlist.model.Setlist
import com.bandage.v1.domain.setlist.model.SetlistBand
import com.bandage.v1.domain.setlist.model.SetlistTrack
import com.bandage.v1.domain.setlist.model.SetlistTrackParticipant
import com.bandage.v1.domain.setlist.repository.SetlistBandRepository
import com.bandage.v1.domain.setlist.repository.SetlistRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackRepository
import com.bandage.v1.global.common.domain.TrackInfo
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
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
                        sessions = item.sessions,
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
