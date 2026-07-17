package com.bandage.bandmanager.domain.schedule_bak.service

import com.bandage.bandmanager.domain.availability.model.AvailabilityException
import com.bandage.bandmanager.domain.availability.model.AvailabilityKind
import com.bandage.bandmanager.domain.availability.model.MemberAvailability
import com.bandage.bandmanager.domain.availability.repository.MemberAvailabilityRepository
import com.bandage.bandmanager.domain.schedule_bak.repository.MemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 레거시 MemberSchedule(회의 단위 가용성) → MemberAvailability(글로벌 가용성) 전환 서비스 (PRD-2 T15).
 *
 * 회의별로 흩어진 availableDates/unavailableDates 를 멤버 단위로 모아 날짜별 전일(AVAILABLE/BLOCKED) 예외로 옮긴다.
 * 같은 날짜가 가용/불가에 동시에 존재하면 보수적으로 BLOCKED 가 우선한다.
 *
 * 주의: 슬롯 단위 blocks(hex 비트맵)는 본 마이그레이션에서 전일 예외로 단순화하지 않고 생략한다(별도 정밀 이관 대상).
 * 운영 데이터가 비어 있는 현 시점에서는 일괄 실행이 no-op 에 가깝다.
 */
@Service
class MemberScheduleMigrationService(
    private val memberScheduleRepository: MemberScheduleRepository,
    private val memberAvailabilityRepository: MemberAvailabilityRepository,
) {
    /** 단일 멤버 전환. 전환된 예외 개수를 반환. */
    @Transactional
    fun migrateMember(memberId: Long): Int {
        val schedules = memberScheduleRepository.findAllByUserId(memberId)
        if (schedules.isEmpty()) return 0

        val unavailable = schedules.flatMap { it.unavailableDates }.toSet()
        val available = schedules.flatMap { it.availableDates }.toSet() - unavailable

        val migratedExceptions =
            available.map { AvailabilityException(it, AvailabilityKind.AVAILABLE, null, null) } +
                unavailable.map { AvailabilityException(it, AvailabilityKind.BLOCKED, null, null) }
        if (migratedExceptions.isEmpty()) return 0

        val availability =
            memberAvailabilityRepository.findByMemberId(memberId)
                ?: MemberAvailability.create(memberId)

        // 기존 예외와 날짜+종류 기준으로 중복 제거 병합
        val existingKeys = availability.exceptions.map { it.date to it.kind }.toSet()
        val merged =
            availability.exceptions +
                migratedExceptions.filter { (it.date to it.kind) !in existingKeys }
        availability.updateExceptions(merged)

        memberAvailabilityRepository.save(availability)
        return migratedExceptions.size
    }

    /** 전체 멤버 일괄 전환. (멤버 수, 전환 예외 총합) 반환. */
    @Transactional
    fun migrateAll(): MigrationResult {
        val memberIds = memberScheduleRepository.findAll().map { it.userId }.distinct()
        var totalExceptions = 0
        memberIds.forEach { totalExceptions += migrateMember(it) }
        return MigrationResult(migratedMembers = memberIds.size, migratedExceptions = totalExceptions)
    }

    data class MigrationResult(
        val migratedMembers: Int,
        val migratedExceptions: Int,
    )
}
