package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.schedule.dto.req.ScheduleAutoPlaceRequest
import com.bandage.bandmanager.domain.schedule.dto.res.ScheduleBlockResponse
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBlockRepository
import com.bandage.bandmanager.domain.schedule.repository.ScheduleBoardRepository
import com.bandage.bandmanager.domain.setlist.repository.SetlistTrackRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ScheduleAutoPlaceService(
    private val ScheduleBoardRepository: ScheduleBoardRepository,
    private val ScheduleBlockRepository: ScheduleBlockRepository,
    private val SetlistTrackRepository: SetlistTrackRepository,
) {
    fun autoPlaceScheduleBlocks(
        setlistId: UUID,
        boardId: UUID,
        request: ScheduleAutoPlaceRequest,
    ): List<ScheduleBlockResponse> {
        val resultList = mutableListOf<ScheduleBlockResponse>()
        /**
         * 0. 정의
         * 슬롯: 하루를 30분 단위 48칸으로 나눈 것. 블록 구간은 [startSlot, endSlot) 반열린 구간으로 표현한다.
         * 동시 배치 불가 조건: 같은 슬롯에, 하나의 셋리스트 참여 멤버가 동시에 2개 이상의 스케줄 블럭을 가질 수 없다.
         * 배치 단위: 길이 request.jamDurationSlots 인 연속 구간 1개. 이 구간이 배치의 최소 단위다.
         * 가용 판정: 해당 트랙의 참여 멤버 전원이 구간 전체에 가용해야 한다.
         *            가용성 미등록 멤버는 제약 없음(항상 가용)으로 본다.
         * 회차: interval 이 ONCE 가 아니면 윈도우를 interval 단위로 쪼갠 각 구간이 1회차이며,
         *       각 트랙은 회차당 최대 1회 배치된다. (recurrence 는 사용하지 않고 블록을 회차 수만큼 실제 생성한다)
         *
         * 1. 리소스 확보
         * request 의 각 조건에 기본적인 validation 을 적용한다.
         *   - validateTimePreference() (startTimePreference < endTimePreference)
         *   - 윈도우(windowFrom/windowTo)가 존재하는지, from <= to 인지
         *   - 윈도우 길이가 interval 을 담을 수 있는지 (예: 윈도우 2주 이하면 MONTHLY 불가, 하루 이하면 자동배치 미수행)
         * 기존 셋리스트+스케줄보드에 속한 스케줄 블록 중 고정된 스케줄 블록이 배치된 슬롯 목록(pinnedScheduleBlock, 이하 psbl) 을 조회한다.
         *   - 각 블록은 [startSlot, endSlot) 구간이므로 슬롯 단위로 펼쳐 보유한다.
         *   - pinned = false 인 기존 블록은 재배치 대상이므로 psbl 에 포함하지 않고 이번 배치로 덮어쓴다.
         *
         * 셋리스트 아이템 목록 + 각 아이템에 속한 멤버 + 각 멤버의 가용성 정보를 조회한다.
         * 스케줄보드의 윈도우 범위 안에서, 각 셋리스트 아이템이 배치될 수 있는 슬롯 목록(availableSlotList, 이하 asl)을 아이템별로 조회한다.
         *   - 후보는 jamDurationSlots 길이의 연속 구간이며, 참여 멤버 전원이 그 구간 전체에 가용해야 한다.
         * 요구사항 충족을 위해, TimePreference 범위로 좁힌 슬롯 목록을 (preferredSlotList, 이하 psl) 아이템별로 산출한다.
         * 각 아이템별 psl 의 요소가 최소 1개 이상인지 확인한다.
         * psl 의 요소가 0개인 아이템의 경우, asl 로 psl 을 덮어쓴다. (이때도 0개인 경우 auto place 범위에서 제외)
         * -> 이 경우, 한번 psl 에서 배치 가능 대상이 0개로 판명된 경우 fellBack = true 로 필드값 업데이트, 이후, 정렬 시 fellBack 먼저 처리
         *
         * 2. 배치
         * Interval 단위로 fellBack=true -> psl 의 사이즈가 작은 순으로 psl 을 담은 리스트를 순회하며, 아래와 같이 배치를 시작한다:
         * psl 을 dayPreference 에 따라 정렬한다.
         * psbl 의 슬롯과 겹치는 멤버가 있다면 리스트에서 배제한다.
         * 해당 psl 을 순회하며 스케줄 보드의 interval 단위 내에 다른 셋리스트 아이템이 배치되어 있지 않은지 확인하고,
         * 배치 가능한 슬롯 중 dayPreference 우선순위가 가장 높은 요일을 먼저 취하고,
         * 그 요일 안에서 해당 시간대에 배치했을 때 다른 아이템의 배치가능슬롯 수가 가장 적게 감소되는 위치에 셋리스트 아이템을 배치한다.
         * (감소량이 동일하면 더 이른 시간을 택한다.)
         * 배치 가능한 슬롯을 찾았다면, 해당 날짜에 기 배치된 슬롯들을 확인하여, 해당 블록의 셋리스트 아이템에 참여중인 멤버들의 가용성을 확인한다.
         * maxJamsPerDay, maxEmptySlotsBetweenJams 을 위반한다면, 해당 블록에 배치하지 않고 넘어간다.
         *   - 두 제약 모두 보드 전체가 아니라 해당 트랙의 참여 멤버 각각을 기준으로 판정한다.
         * 배치된 슬롯은 리스트에서 삭제하고, 스케줄 블록을 생성해 결과 리스트에 담는다.
         * 해당 회차에서 끝내 배치되지 못한 아이템은 그 회차만 건너뛰고, 다음 회차에서 다시 시도한다.
         * 전체 psl 들에 대한 순회가 끝났다면, 다음 interval 단위에 대해 동일한 작업을 반복한다.
         *
         * 3. 결과 반영
         * 배치가 완료된 뒤, 보드-트랙 단위 배치 결과를 ScheduleBoardSetlistItemPlacement 에 업데이트한다.
         *   - 배치된 트랙: 배치 횟수(= 성공한 회차 수)를 기록한다.
         *   - 단 한번도 배치되지 못한 트랙: 미배치로 기록한다. (psl/asl 이 0개여서 제외된 트랙 포함)
         *   - 블록의 실제 시간 정보는 ScheduleBlockTrack 이 보유하므로 여기서는 중복 저장하지 않는다.
         * 결과를 반환한다.
         */
        return resultList
    }
}
