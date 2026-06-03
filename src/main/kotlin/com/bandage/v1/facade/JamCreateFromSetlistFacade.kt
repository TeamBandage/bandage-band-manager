package com.bandage.v1.facade

import com.bandage.v1.domain.jam.dto.req.SetlistToJamRequest
import com.bandage.v1.domain.jam.dto.res.JamResponse
import com.bandage.v1.domain.jam.service.SetlistTrackToJamConverter
import com.bandage.v1.domain.setlist.repository.SetlistRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackParticipantRepository
import com.bandage.v1.domain.setlist.repository.SetlistTrackRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 확정된 Setlist를 합주(Jam)로 전파한다.
 * SetlistTrack 1건 → Jam 1건. 실제 변환은 [SetlistTrackToJamConverter] 에 위임하여
 * ScheduleConfirmFacade(Task 9) 와 동일한 변환 규칙을 공유한다.
 */
@Service
class JamCreateFromSetlistFacade(
    private val setlistRepository: SetlistRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val setlistTrackParticipantRepository: SetlistTrackParticipantRepository,
    private val setlistTrackToJamConverter: SetlistTrackToJamConverter,
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
        val participantsByTrack =
            setlistTrackParticipantRepository
                .findAllByTrackIn(tracks)
                .groupBy { it.track.id }

        return setlistTrackToJamConverter
            .toJams(
                tracks = tracks,
                participantsMap = participantsByTrack,
                startAt = request.startAt,
                durationMinutes = request.durationMinutes,
                venue = request.venue,
            ).map { JamResponse.of(it) }
    }
}
