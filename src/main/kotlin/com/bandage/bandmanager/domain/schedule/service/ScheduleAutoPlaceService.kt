package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleAutoPlaceRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBlockResponse
import com.bandage.bandmanager.domain.schedule.model.ScheduleBoard
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardSetlistItemPlacementRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleAutoPlaceService(
    private val scheduleBoardRepository: ScheduleBoardRepository,
    private val scheduleBlockRepository: ScheduleBlockRepository,
    private val setlistTrackRepository: SetlistTrackRepository,
    private val placementRepository: ScheduleBoardSetlistItemPlacementRepository,
    private val scheduleAuthService: ScheduleAuthService,
) {
    @Transactional
    fun autoPlaceScheduleBlocks(
        setlistId: UUID,
        boardId: UUID,
        memberId: Long,
        request: ScheduleAutoPlaceRequest,
    ): List<ScheduleBlockResponse> {
        scheduleAuthService.validateSetlistManager(setlistId, memberId)
        val board = getBoardOrThrow(setlistId, boardId)
        if (board.confirmed) throw BusinessException(ErrorCode.SCHEDULE_BOARD_ALREADY_CONFIRMED)
        request.validateTimePreference()
        board.scheduleWindowOrNull() ?: throw BusinessException(ErrorCode.SCHEDULE_WINDOW_REQUIRED)

        val resultList = mutableListOf<ScheduleBlockResponse>()
        /**
         * 0. 정의
         * 슬롯: 하루를 30분 단위 48칸으로 나눈 것. 블록 구간은 [startSlot, endSlot) 반열린 구간으로 표현한다.
         *       startSlot 은 0..47, endSlot 은 1..48 이며 이 규약은 Slot / 가용성 / 보드 시간대가 모두 공유한다.
         * 동시 배치 불가 조건: 같은 슬롯에, 하나의 셋리스트 참여 멤버가 동시에 2개 이상의 스케줄 블럭을 가질 수 없다.
         *
         * 잼 그룹(이하 그룹): 참여 멤버 집합이 완전히 동일한 트랙들을 하나로 묶은 것. 배치의 단위다.
         *   - 트랙 단위로 배치하면, 멤버 구성이 같은 두 트랙은 위 동시 배치 불가 조건 때문에 서로 다른 시간대로
         *     강제로 흩어진다. 같은 사람들이 두 번 모여야 하므로 현실의 합주와 맞지 않는다.
         *   - 따라서 같은 멤버들이 모인 한 자리에서 여러 곡을 이어 연습하도록 묶어서 배치하고,
         *     ScheduleBlockTrack 의 N:M 매핑으로 블록 1개에 트랙 여러 개를 연결한다.
         *   - 멤버 집합이 부분만 겹치는 트랙은 묶지 않는다. (묶음 조합 최적화는 이 단계의 범위 밖)
         *
         * 배치 단위: 길이 (request.jamDurationSlots * 그룹 내 트랙 수) 인 연속 구간 1개.
         * 가용 판정: 해당 그룹의 참여 멤버 전원이 구간 전체에 가용해야 한다.
         *            가용성 미등록 멤버는 제약 없음(항상 가용)으로 본다.
         * maxJamsPerDay / maxEmptySlotsBetweenJams: 곡이 아니라 "모임(블록)" 단위로 센다.
         *            즉 한 그룹이 한 블록에 3곡을 묶어 연습해도 그날의 잼 횟수는 1이다.
         * 회차: interval 이 ONCE 가 아니면 윈도우를 interval 단위로 쪼갠 각 구간이 1회차이며,
         *       각 그룹은 회차당 최대 1회 배치된다. (recurrence 는 사용하지 않고 블록을 회차 수만큼 실제 생성한다)
         *
         * 1. 리소스 확보
         * 권한/상태/요청 검증은 이 메서드 진입부에서 이미 수행했다(매니저 권한, confirmed 여부,
         * TimePreference 정합성, 윈도우 존재). 아래는 그 이후 단계다.
         *   - 남은 검증: 윈도우 길이가 interval 을 담을 수 있는지
         *     (예: 윈도우 2주 이하면 MONTHLY 불가, 하루 이하면 자동배치 미수행)
         * 기존 셋리스트+스케줄보드에 속한 스케줄 블록 중 고정된 스케줄 블록이 배치된 슬롯 목록(pinnedScheduleBlock, 이하 psbl) 을 조회한다.
         *   - 각 블록은 [startSlot, endSlot) 구간이므로 슬롯 단위로 펼쳐 보유한다.
         *   - pinned = false 인 기존 블록은 재배치 대상이므로 psbl 에 포함하지 않고 이번 배치로 덮어쓴다.
         *
         * 셋리스트 아이템 목록 + 각 아이템에 속한 멤버 + 각 멤버의 가용성 정보를 조회한다.
         * 참여 멤버 집합이 동일한 트랙끼리 묶어 그룹 목록을 만든다. 이후 배치는 트랙이 아닌 그룹 단위로 진행한다.
         * 스케줄보드의 윈도우(날짜 범위) 안에서, 각 그룹이 배치될 수 있는 슬롯 목록(availableSlotList, 이하 asl)을 그룹별로 조회한다.
         *   - 후보는 (jamDurationSlots * 그룹 내 트랙 수) 길이의 연속 구간이며, 참여 멤버 전원이 그 구간 전체에 가용해야 한다.
         *   - 하루 안의 시간대는 보드의 boardTimeRangeFrom/To 로 먼저 좁힌다. (하드 제약)
         * 요구사항 충족을 위해, TimePreference 범위로 좁힌 슬롯 목록을 (preferredSlotList, 이하 psl) 그룹별로 산출한다.
         *   - TimePreference 도 하드 제약이다. 이 범위 밖에는 배치하지 않는다.
         * 각 그룹별 psl 의 요소가 최소 1개 이상인지 확인한다.
         * psl 의 요소가 0개인 그룹의 경우, asl 로 psl 을 덮어쓴다. (이때도 0개인 경우 auto place 범위에서 제외)
         * -> 이 경우, 한번 psl 에서 배치 가능 대상이 0개로 판명된 경우 fellBack = true 로 필드값 업데이트, 이후, 정렬 시 fellBack 먼저 처리
         *   - 이 폴백이 푸는 것은 dayPreference(요일 선호)뿐이며, 시간대 제약은 그대로 유지된다.
         *     즉 asl 자체가 이미 boardTimeRange 와 TimePreference 안에서만 만들어져 있어야 한다.
         *     (시간대까지 풀어버리면 사용자가 의도하지 않은 새벽 시간에 배치될 수 있다)
         *
         * 2. 배치
         * Interval 단위로 fellBack=true -> psl 의 사이즈가 작은 순으로 psl 을 담은 리스트를 순회하며, 아래와 같이 배치를 시작한다:
         * psl 을 dayPreference 에 따라 정렬한다.
         * psbl 의 슬롯과 겹치는 멤버가 있다면 리스트에서 배제한다.
         * 해당 psl 을 순회하며 스케줄 보드의 interval 단위 내에 다른 그룹이 배치되어 있지 않은지 확인하고,
         * 배치 가능한 슬롯 중 dayPreference 우선순위가 가장 높은 요일을 먼저 취하고,
         * 그 요일 안에서 해당 시간대에 배치했을 때 다른 그룹의 배치가능슬롯 수가 가장 적게 감소되는 위치에 그룹을 배치한다.
         * (감소량이 동일하면 더 이른 시간을 택한다.)
         * 배치 가능한 슬롯을 찾았다면, 해당 날짜에 기 배치된 슬롯들을 확인하여, 해당 그룹에 참여중인 멤버들의 가용성을 확인한다.
         * maxJamsPerDay, maxEmptySlotsBetweenJams 을 위반한다면, 해당 블록에 배치하지 않고 넘어간다.
         *   - 두 제약 모두 보드 전체가 아니라 해당 그룹의 참여 멤버 각각을 기준으로 판정한다.
         * 배치된 슬롯은 리스트에서 삭제하고, 스케줄 블록을 생성해 결과 리스트에 담는다.
         *   - 그룹 내 모든 트랙을 ScheduleBlockTrack 으로 해당 블록에 연결한다(ordinal 은 그룹 내 순서).
         *   - placementOrigin 은 AUTO 로 기록한다.
         * 해당 회차에서 끝내 배치되지 못한 그룹은 그 회차만 건너뛰고, 다음 회차에서 다시 시도한다.
         * 전체 psl 들에 대한 순회가 끝났다면, 다음 interval 단위에 대해 동일한 작업을 반복한다.
         *
         * 3. 결과 반영
         * 배치가 완료된 뒤, 보드-트랙 단위 배치 결과를 ScheduleBoardSetlistItemPlacement 에 업데이트한다.
         *   - 그룹이 배치될 때마다 그룹에 속한 모든 트랙의 placementCount 를 함께 증가시킨다.
         *     (한 블록에 3곡이 묶였다면 3개 트랙 모두 +1)
         *   - 단 한번도 배치되지 못한 트랙: placementCount = 0 으로 기록한다. (psl/asl 이 0개여서 제외된 트랙 포함)
         *   - 블록의 실제 시간 정보는 ScheduleBlockTrack 이 보유하므로 여기서는 중복 저장하지 않는다.
         *   - 재실행 시 보드 단위로 전체 교체한다(deleteAllByBoardId 후 재생성).
         * 결과를 반환한다.
         */
        return resultList
    }

    private fun getBoardOrThrow(
        setlistId: UUID,
        boardId: UUID,
    ): ScheduleBoard {
        val board =
            scheduleBoardRepository.findByIdOrNull(boardId)
                ?: throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        if (board.setlistId != setlistId) {
            throw BusinessException(ErrorCode.SCHEDULE_BOARD_NOT_FOUND)
        }
        return board
    }
}
