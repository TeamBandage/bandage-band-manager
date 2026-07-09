package com.bandage.bandmanager.domain.jam.service

import com.bandage.bandmanager.domain.jam.model.Jam
import com.bandage.bandmanager.domain.jam.repository.JamRepository
import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * SetlistTrack → Jam 변환을 단일화하는 컴포넌트.
 *
 * JamCreateFromSetlistFacade(셋리스트 → 합주 전파)와 ScheduleConfirmFacade(시간표 확정, Task 9)가
 * 동일한 변환 로직을 공유하도록 하여, 변환 규칙의 중복/표류를 방지한다.
 *
 * 변환 규칙:
 * - SetlistTrack.trackInfo → Jam.trackInfo (필드 복사)
 * - SetlistTrack.sessions → Jam.sessions (SessionDef 새 인스턴스로 깊은 복사, 공유 방지)
 * - SetlistTrackParticipant(sessionId, memberId) → JamParticipant
 * - Jam.setlistId 에 출처 Setlist 기록
 * - 저장 후 JamReservationSyncService.sync() 로 예약 동기화
 */
@Component
@Transactional(readOnly = true)
class SetlistTrackToJamConverter(
    private val jamRepository: JamRepository,
    private val jamReservationSyncService: JamReservationSyncService,
) {
    @Transactional
    fun toJam(
        track: SetlistTrack,
        participants: List<SetlistTrackParticipant>,
        startAt: LocalDateTime,
        durationMinutes: Int,
        venue: String?,
        titleOverride: String? = null,
    ): Jam {
        val source = track.trackInfo
        val jam =
            Jam.create(
                title = titleOverride?.takeIf { it.isNotBlank() } ?: source.title,
                trackInfo =
                    TrackInfo(
                        title = source.title,
                        artist = source.artist,
                        album = source.album,
                        duration = source.duration,
                        reference = source.reference,
                    ),
                startAt = startAt,
                durationMinutes = durationMinutes,
                venue = venue,
                note = track.note,
                setlistId = track.setlist.id,
                sessions =
                    track.sessions.map { def ->
                        SessionDef(
                            sessionId = def.sessionId,
                            label = def.label,
                            short = def.short,
                            custom = def.custom,
                        )
                    },
            )

        participants.forEach { participant ->
            jam.addParticipant(participant.sessionId, participant.memberId)
        }

        val savedJam = jamRepository.save(jam)
        jamReservationSyncService.sync(savedJam)
        return savedJam
    }

    @Transactional
    fun toJams(
        tracks: List<SetlistTrack>,
        participantsMap: Map<UUID, List<SetlistTrackParticipant>>,
        startAt: LocalDateTime,
        durationMinutes: Int,
        venue: String?,
    ): List<Jam> =
        tracks.map { track ->
            toJam(
                track = track,
                participants = participantsMap[track.id] ?: emptyList(),
                startAt = startAt,
                durationMinutes = durationMinutes,
                venue = venue,
            )
        }
}
