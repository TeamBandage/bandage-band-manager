package com.bandage.bandmanager.domain.availability.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate

/**
 * 멤버의 글로벌 가용성. 특정 회의/공연에 종속되지 않는 멤버 본인의 상시 가용 정보다.
 *
 * PK 는 memberId 자체(auto-gen 아님)이며, 멤버 1명당 1건만 존재한다(upsert).
 * 주간 반복 규칙(WeeklyRule)과 날짜별 예외(AvailabilityException)로 구성된다.
 */
@Entity
@Table(name = "p_member_availability")
@SQLRestriction("deleted_at IS NULL")
open class MemberAvailability(
    memberId: Long,
    note: String? = null,
) : BaseEntity() {
    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_member_availability_weekly_rules",
        joinColumns = [JoinColumn(name = "member_id", referencedColumnName = "member_id")],
    )
    private var _weeklyRules: MutableList<WeeklyRule> = mutableListOf()
    val weeklyRules: List<WeeklyRule> get() = _weeklyRules.toList()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_member_availability_exceptions",
        joinColumns = [JoinColumn(name = "member_id", referencedColumnName = "member_id")],
    )
    private var _exceptions: MutableList<AvailabilityException> = mutableListOf()
    val exceptions: List<AvailabilityException> get() = _exceptions.toList()

    @Column(name = "note", nullable = true, length = 500)
    var note: String? = note
        protected set

    companion object {
        fun create(
            memberId: Long,
            note: String? = null,
        ): MemberAvailability =
            MemberAvailability(
                memberId = memberId,
                note = note,
            )
    }

    fun updateWeeklyRules(rules: List<WeeklyRule>) {
        _weeklyRules.clear()
        _weeklyRules.addAll(rules)
    }

    fun updateExceptions(exceptions: List<AvailabilityException>) {
        _exceptions.clear()
        _exceptions.addAll(exceptions)
    }

    fun updateNote(note: String?) {
        this.note = note
    }

    /**
     * 특정 날짜의 요청 구간 [reqStart, reqEnd) 가 가용한지 판정한다.
     * 우선순위: BLOCKED 예외(겹치면 불가) > AVAILABLE 예외(포함하면 가용) > 주간 규칙(완전히 덮으면 가용).
     * 가용성/충돌 계산(AvailabilityCalculator)과 슬롯 전개 양쪽이 공유하는 단일 판정 로직이다.
     */
    fun isAvailableAt(
        date: LocalDate,
        reqStart: Int,
        reqEnd: Int,
    ): Boolean {
        val dayExceptions = _exceptions.filter { it.date == date }
        if (dayExceptions.any { it.kind == AvailabilityKind.BLOCKED && it.overlaps(reqStart, reqEnd) }) return false
        if (dayExceptions.any { it.kind == AvailabilityKind.AVAILABLE && it.covers(reqStart, reqEnd) }) return true
        return _weeklyRules.any { it.isEffectiveOn(date) && it.startSlot <= reqStart && reqEnd <= it.endSlot }
    }
}
