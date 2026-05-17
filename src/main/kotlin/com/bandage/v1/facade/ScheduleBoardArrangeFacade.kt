package com.bandage.v1.facade

import com.bandage.v1.domain.performance.model.Performance
import com.bandage.v1.domain.performance.model.PerformancePractice
import com.bandage.v1.domain.performance.repository.PerformancePracticeRepository
import com.bandage.v1.domain.performance.repository.PerformanceRepository
import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeSong
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.domain.practice.repository.PracticeSongRepository
import com.bandage.v1.domain.schedule.dto.res.ScheduleBoardResponse
import com.bandage.v1.domain.schedule.model.ScheduleBlock
import com.bandage.v1.domain.schedule.model.ScheduleBoard
import com.bandage.v1.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.v1.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.v1.domain.schedule.service.ScheduleAuthService
import com.bandage.v1.domain.setlist.model.SetlistItem
import com.bandage.v1.domain.setlist.model.SetlistMeeting
import com.bandage.v1.domain.setlist.model.SetlistMeetingMember
import com.bandage.v1.domain.setlist.repository.SetlistItemRepository
import com.bandage.v1.domain.setlist.repository.SetlistMeetingMemberRepository
import com.bandage.v1.domain.setlist.repository.SetlistMeetingRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ScheduleBoardArrangeFacade(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val setlistMeetingRepository: SetlistMeetingRepository,
    private val setlistMeetingMemberRepository: SetlistMeetingMemberRepository,
    private val setlistItemRepository: SetlistItemRepository,
    private val practiceSongRepository: PracticeSongRepository,
    private val practiceRepository: PracticeRepository,
    private val performanceRepository: PerformanceRepository,
    private val performancePracticeRepository: PerformancePracticeRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun setupInitialScheduleBoard(
        meetingId: UUID,
        memberId: Long,
        suggestionQty: Int,
    ): ScheduleBoardResponse = ScheduleBoardResponse.of()

    private fun setupScheduleBlock(meetingId: UUID) {
        val setlist = getSetlist(meetingId)
        val setlistItemList = setlistItemRepository.findAllByMeeting(setlist)

        // 시간표 자동 배치는 "1주일"을 기준으로 배치
        // 시간표 블럭: 몇시간 할 것인지? 정해야 함. 엔티티 수정 필요
        // setlistItem 별 duration: 해당 곡의 지속 시간
        // 1시간 단위로 합주 테이블 블럭 사이즈 잡고, 만약 duration이 1시간을 넘어가면 1시간 단위로 ++
        // 1. 배치 가능한 시간 확인(Working Hours), 반복 가능한 주차
        // 2. 배치해야 하는 각 곡별 참여인원 확인 -> 교집합 시간 찾기
        // 3. 참여 인원이 가장 많은 곡(전체 합주)이나 난이도가 높은 곡을 항상 앞쪽에 오도록 정렬
    }

    private fun getScheduleBoard(boardId: UUID): ScheduleBoard =
        scheduleBoardRepository.findByIdOrNull(boardId)
            ?: throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)

    private fun getScheduleBlock(blockId: UUID): ScheduleBlock =
        scheduleBlockRepository.findByIdOrNull(blockId)
            ?: throw BusinessException(ErrorCode.SCHEDULE_BLOCK_NOT_FOUND)

    private fun getSetlist(meetingId: UUID): SetlistMeeting =
        setlistMeetingRepository.findByIdOrNull(meetingId)
            ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)

    private fun getSetlistMeetingMember(
        meeting: SetlistMeeting,
        memberId: Long,
    ): SetlistMeetingMember =
        setlistMeetingMemberRepository.findByMeetingAndMemberId(meeting, memberId)
            ?: throw BusinessException(ErrorCode.RESOURCE_NOT_FOUND)

    private fun getSetlistItem(itemId: UUID): SetlistItem =
        setlistItemRepository.findByIdOrNull(itemId)
            ?: throw BusinessException(ErrorCode.SETLIST_ITEM_NOT_FOUND)

    private fun getPracticeSong(songId: UUID): PracticeSong =
        practiceSongRepository.findByIdOrNull(songId)
            ?: throw BusinessException(ErrorCode.PRACTICE_SONG_NOT_FOUND)

    private fun getPractice(practiceId: UUID): Practice =
        practiceRepository.findByIdOrNull(practiceId)
            ?: throw BusinessException(ErrorCode.PRACTICE_NOT_FOUND)

    private fun getPerformance(performanceId: UUID): Performance =
        performanceRepository.findByIdOrNull(performanceId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)

    private fun getPerformancePractice(performancePracticeId: UUID): PerformancePractice =
        performancePracticeRepository.findByIdOrNull(performancePracticeId)
            ?: throw BusinessException(ErrorCode.PERFORMANCE_PRACTICE_NOT_FOUND)
}
