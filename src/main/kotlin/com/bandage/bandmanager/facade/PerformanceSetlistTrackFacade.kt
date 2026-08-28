package com.bandage.bandmanager.facade

import com.bandage.bandmanager.domain.performance.dto.res.PerformanceSetlistTracksResponse
import com.bandage.bandmanager.domain.performance.service.PerformanceService
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.domain.setlist.service.SetlistService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 공연에 묶인 모든 셋리스트의 트랙·참여자를 조회한다(BD-264).
 *
 * 셋리스트 단위 접근 권한("매니저 OR 트랙 참여자")과 달리, 공연에 묶인 셋리스트의 트랙·참여자는
 * 공연 비참여자를 포함해 누구나 조회할 수 있다(BD-279). 조회 대상 판정의 근거는
 * PerformanceSetlist 연결이므로 performance 도메인이 맡고
 * ([PerformanceService.getSetlistIds]), setlist 도메인은 조립만 담당한다
 * ([SetlistService.getTracksBySetlistIds], [SetlistService.getParticipantsBySetlistIds]).
 * 덕분에 setlist 도메인에 공연 개념을 들이지 않고 모듈 경계가 유지되며,
 * 셋리스트 단위 API 의 권한 정의도 그대로 남는다.
 *
 * 넓어지는 것은 "누가 볼 수 있는가"뿐이다. 각 셋리스트의 참여자 목록 자체는 기존 정의
 * (그 셋리스트의 매니저 + 트랙 배정자)를 유지하므로, 공연에 묶였다는 이유만으로
 * 트랙 배정 없는 인원이 남의 참여자 목록에 섞이지 않는다.
 */
@Service
class PerformanceSetlistTrackFacade(
    private val performanceService: PerformanceService,
    private val setlistService: SetlistService,
    private val setlistRepository: SetlistRepository,
) {
    @Transactional(readOnly = true)
    fun getSetlistTracks(performanceId: UUID): List<PerformanceSetlistTracksResponse> {
        val setlistIds = performanceService.getSetlistIds(performanceId)
        if (setlistIds.isEmpty()) return emptyList()

        // 소프트 삭제된 셋리스트는 @SQLRestriction 으로 걸러지므로 조회 결과에만 의존한다.
        val setlists = setlistRepository.findAllById(setlistIds)
        val tracksBySetlist = setlistService.getTracksBySetlistIds(setlists.map { it.id })
        val participantsBySetlist = setlistService.getParticipantsBySetlistIds(setlists)

        return setlists.map {
            PerformanceSetlistTracksResponse.of(
                setlist = it,
                tracks = tracksBySetlist[it.id].orEmpty(),
                participants = participantsBySetlist[it.id].orEmpty(),
            )
        }
    }
}
