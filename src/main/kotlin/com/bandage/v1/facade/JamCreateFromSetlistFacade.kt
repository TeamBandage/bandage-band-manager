package com.bandage.v1.facade

import com.bandage.v1.domain.jam.dto.req.SetlistToJamRequest
import com.bandage.v1.domain.jam.dto.res.JamResponse
import com.bandage.v1.domain.jam.model.Jam
import com.bandage.v1.domain.jam.repository.JamRepository
import com.bandage.v1.domain.setlist.repository.SetlistRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackRepository
import com.bandage.v1.global.common.domain.SessionDef
import com.bandage.v1.global.common.domain.TrackInfo
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 확정된 Setlist를 합주(Jam)로 전파한다.
 * SetlistTrack 1건 → Jam 1건, SetlistTrackParticipant(sessionId, memberId) → JamParticipant 로 복사하며,
 * 생성된 Jam 의 setlistId 에 출처 Setlist 를 기록한다.
 */
@Service
class JamCreateFromSetlistFacade(
    private val setlistRepository: SetlistRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
    private val jamRepository: JamRepository,
) {
    @Transactional
    fun createJamsFromSetlist(
        memberId: Long,
        setlistId: UUID,
        request: SetlistToJamRequest,
    ): List<JamResponse> {
        val setlist =
            setlistRepository.findByIdOrNull(setlistId)
                ?: throw BusinessException(ErrorCode.SETLIST_NOT_FOUND)
        if (setlist.managerId != memberId) {
            throw BusinessException(ErrorCode.SETLIST_NOT_MANAGER)
        }

        val tracks = setlistTrackRepository.findAllBySetlist(setlist)
        if (tracks.isEmpty()) {
            throw BusinessException(ErrorCode.SETLIST_NO_SELECTED_TRACK)
        }
        val participantsByTrack = setlistTrackParticipantRepository.findAllByTrackIn(tracks).groupBy { it.track.id }

        return tracks.map { track ->
            val jam =
                Jam.create(
                    title = track.trackInfo.title,
                    trackInfo =
                        TrackInfo(
                            title = track.trackInfo.title,
                            artist = track.trackInfo.artist,
                            album = track.trackInfo.album,
                            duration = track.trackInfo.duration,
                            reference = track.trackInfo.reference,
                        ),
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                    venue = request.venue,
                    note = track.note,
                    setlistId = setlist.id,
                    sessions =
                        track.sessions.map { def ->
                            SessionDef(
                                sessionId = def.sessionId,
                                label = def.label,
                                short = def.short,
                                need = def.need,
                                custom = def.custom,
                            )
                        },
                )
            participantsByTrack[track.id]?.forEach { participant ->
                jam.addParticipant(participant.sessionId, participant.memberId)
            }
            jamRepository.save(jam)
            JamResponse.of(jam)
        }
    }
}
