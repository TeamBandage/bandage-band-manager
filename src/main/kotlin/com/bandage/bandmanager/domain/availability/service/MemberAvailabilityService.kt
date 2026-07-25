package com.bandage.bandmanager.domain.availability.service

import com.bandage.bandmanager.domain.availability.dto.req.MemberAvailabilityRequest
import com.bandage.bandmanager.domain.availability.dto.res.MemberAvailabilityResponse
import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.model.WeeklyRule
import com.bandage.bandmanager.domain.availability.model.WeeklyRule.Companion.SLOTS_PER_DAY
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.global.common.response.ScheduleSlotResponse
import com.bandage.bandmanager.global.common.response.ScheduleSlotType
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class MemberAvailabilityService(
    private val memberAvailabilityRepository: MemberAvailabilityRepository,
) {
    /** 내 가용성 조회. 미등록 시 빈 응답 반환. */
    fun getMyAvailability(memberId: Long): MemberAvailabilityResponse =
        memberAvailabilityRepository
            .findByMemberId(memberId)
            ?.let { MemberAvailabilityResponse.from(it) }
            ?: MemberAvailabilityResponse.empty(memberId)

    /**
     * 조회 기간 [from, to] 에 속한 가용 슬롯을 날짜별로 전개해 반환한다.
     * 규칙/예외를 서버에서 구체 슬롯으로 풀어주므로 클라이언트는 추가 계산 없이 그대로 렌더할 수 있다.
     * 미등록 멤버는 빈 리스트. 최대 366일까지 조회 가능.
     */
    fun getMySlots(
        memberId: Long,
        from: LocalDate,
        to: LocalDate,
    ): List<ScheduleSlotResponse> {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw BusinessException(ErrorCode.AVAILABILITY_RANGE_INVALID)
        }
        val availability = memberAvailabilityRepository.findByMemberId(memberId) ?: return emptyList()

        val slots = mutableListOf<ScheduleSlotResponse>()
        var date = from
        while (!date.isAfter(to)) {
            // 하루 48슬롯을 가용 판정에 통과시켜 연속 구간(run-length)으로 묶는다.
            var start = -1
            for (slot in 0 until SLOTS_PER_DAY) {
                val available = availability.isAvailableAt(date, slot, slot + 1)
                if (available && start < 0) start = slot
                if (!available && start >= 0) {
                    slots += slotOf(date, start, slot)
                    start = -1
                }
            }
            if (start >= 0) slots += slotOf(date, start, SLOTS_PER_DAY)
            date = date.plusDays(1)
        }
        return slots
    }

    /**
     * 조회 기간 [from, to] 와 겹치는 주간 규칙/예외 원본을 그대로 반환한다.
     * 슬롯 전개(getMySlots)와 달리 규칙/예외 자체가 필요한 화면(편집 등)을 위한 조회다.
     * - 주간 규칙: 유효 기간이 [from, to] 와 겹치면 포함
     * - 예외: date 가 [from, to] 안이면 포함
     * 미등록 멤버는 빈 응답. 최대 366일까지 조회 가능.
     */
    fun getMyAvailabilityByPeriod(
        memberId: Long,
        from: LocalDate,
        to: LocalDate,
    ): MemberAvailabilityResponse {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw BusinessException(ErrorCode.AVAILABILITY_RANGE_INVALID)
        }
        val availability =
            memberAvailabilityRepository.findByMemberId(memberId)
                ?: return MemberAvailabilityResponse.empty(memberId)

        val rules = availability.weeklyRules.filter { it.overlapsPeriod(from, to) }
        val exceptions = availability.exceptions.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
        return MemberAvailabilityResponse.from(availability, rules, exceptions)
    }

    private fun slotOf(
        date: LocalDate,
        startSlot: Int,
        endSlot: Int,
    ): ScheduleSlotResponse =
        ScheduleSlotResponse(
            date = date,
            startSlot = startSlot,
            endSlot = endSlot,
            type = ScheduleSlotType.AVAILABLE,
        )

    /**
     * 내 가용성 등록/수정(범위 한정 교체). [effectiveFrom, effectiveTo] 구간만 교체하고 구간 밖은 보존한다.
     * - 구간에 걸치는 기존 주간 규칙은 경계에서 잘라낸다(머리/꼬리, 완전 포함이면 제거).
     * - 구간 안의 기존 예외는 제거하고 제출분으로 대체한다.
     * - 잘린 조각/신규 규칙 중 같은 패턴이 맞닿으면 다시 병합(coalesce)해 파편화를 막는다.
     */
    @Transactional
    fun updateMyAvailability(
        memberId: Long,
        request: MemberAvailabilityRequest,
    ): MemberAvailabilityResponse {
        val from = request.effectiveFrom
        val to = request.effectiveTo
        if (from.isAfter(to)) throw BusinessException(ErrorCode.AVAILABILITY_INVALID)

        val newRules = parseWeeklyRules(request)
        val newExceptions = parseExceptions(request)
        if (newExceptions.any { it.date.isBefore(from) || it.date.isAfter(to) }) {
            throw BusinessException(ErrorCode.AVAILABILITY_INVALID)
        }

        val availability =
            memberAvailabilityRepository.findByMemberId(memberId)
                ?: MemberAvailability.create(memberId = memberId)

        val keptRules = availability.weeklyRules.flatMap { clip(it, from, to) }
        val keptExceptions = availability.exceptions.filter { it.date.isBefore(from) || it.date.isAfter(to) }

        availability.updateWeeklyRules(coalesce(keptRules + newRules))
        availability.updateExceptions(keptExceptions + newExceptions)
        availability.updateNote(request.note)

        val saved = memberAvailabilityRepository.save(availability)
        return MemberAvailabilityResponse.from(saved)
    }

    /**
     * 주간 규칙에서 [from, to] 구간을 도려낸다. 반환은 0~2개:
     * - 안 겹침 → 원본 그대로(1개)
     * - 머리/꼬리만 걸침 → 잘린 1개
     * - 구간이 규칙 가운데를 관통 → 머리+꼬리 2개(split)
     * - 규칙이 구간에 완전히 포함 → 0개(제출분으로 대체)
     */
    private fun clip(
        rule: WeeklyRule,
        from: LocalDate,
        to: LocalDate,
    ): List<WeeklyRule> {
        val ruleTo = rule.effectiveTo ?: LocalDate.MAX
        if (ruleTo.isBefore(from) || rule.effectiveFrom.isAfter(to)) return listOf(rule)

        val result = mutableListOf<WeeklyRule>()
        if (rule.effectiveFrom.isBefore(from)) {
            result += WeeklyRule(rule.dayOfWeek, rule.startSlot, rule.endSlot, rule.effectiveFrom, from.minusDays(1))
        }
        if (ruleTo.isAfter(to)) {
            // 원래 무기한(effectiveTo == null)이면 꼬리도 무기한 유지
            result += WeeklyRule(rule.dayOfWeek, rule.startSlot, rule.endSlot, to.plusDays(1), rule.effectiveTo)
        }
        return result
    }

    /**
     * 같은 패턴(dayOfWeek, startSlot, endSlot)의 규칙들 중 날짜 구간이 맞닿거나 겹치는 것을 하나로 병합한다.
     * 동일 패턴을 부분 구간에 다시 칠하거나 clip 으로 쪼개진 조각이 그대로 쌓이는 파편화를 막아,
     * 저장 레벨에서도 멱등이 되게 한다. (무기한 effectiveTo == null 꼬리 포함)
     */
    private fun coalesce(rules: List<WeeklyRule>): List<WeeklyRule> =
        rules
            .groupBy { Triple(it.dayOfWeek, it.startSlot, it.endSlot) }
            .flatMap { (pattern, group) -> mergeContiguous(pattern, group) }

    private fun mergeContiguous(
        pattern: Triple<DayOfWeek, Int, Int>,
        group: List<WeeklyRule>,
    ): List<WeeklyRule> {
        val sorted = group.sortedWith(compareBy({ it.effectiveFrom }, { it.effectiveTo ?: LocalDate.MAX }))
        val merged = mutableListOf<WeeklyRule>()
        var from = sorted.first().effectiveFrom
        var to: LocalDate? = sorted.first().effectiveTo
        for (rule in sorted.drop(1)) {
            val curTo = to
            val ruleTo = rule.effectiveTo
            // 무기한 꼬리(curTo == null)는 이후 전부 흡수. 그 외엔 맞닿음(curTo+1)/겹침이면 병합.
            val contiguous = curTo == null || !rule.effectiveFrom.isAfter(curTo.plusDays(1))
            if (contiguous) {
                to = if (curTo == null || ruleTo == null) null else maxOf(curTo, ruleTo)
            } else {
                merged += ruleOf(pattern, from, curTo)
                from = rule.effectiveFrom
                to = ruleTo
            }
        }
        merged += ruleOf(pattern, from, to)
        return merged
    }

    private fun ruleOf(
        pattern: Triple<DayOfWeek, Int, Int>,
        from: LocalDate,
        to: LocalDate?,
    ): WeeklyRule = WeeklyRule(pattern.first, pattern.second, pattern.third, from, to)

    // 슬롯/기간 검증은 Embeddable init 에서 수행되므로, 생성 실패 시 BusinessException 으로 변환한다.
    private fun parseWeeklyRules(request: MemberAvailabilityRequest): List<WeeklyRule> =
        try {
            request.toWeeklyRules()
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.AVAILABILITY_INVALID)
        }

    private fun parseExceptions(request: MemberAvailabilityRequest): List<AvailabilityException> =
        try {
            request.toExceptions()
        } catch (e: IllegalArgumentException) {
            throw BusinessException(ErrorCode.AVAILABILITY_INVALID)
        }

    companion object {
        const val MAX_RANGE_DAYS: Long = 366
    }
}
