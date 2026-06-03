package com.bandage.v1.domain.jam.service

import com.bandage.v1.domain.jam.model.Jam
import com.bandage.v1.domain.jam.model.JamReservation
import com.bandage.v1.domain.jam.repository.JamParticipantRepository
import com.bandage.v1.domain.jam.repository.JamReservationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Jam 생명주기에 맞춰 JamReservation 을 동기화하는 유일한 진입점.
 *
 * delete-reinsert 패턴으로 동작하며, 한 멤버가 같은 Jam 의 여러 세션에 참여하더라도
 * 1건의 예약만 생성한다(시간 구간은 Jam 전체와 동일하므로).
 *
 * 소프트 삭제된 참여자를 제외하기 위해 in-memory 컬렉션이 아닌
 * Repository(@SQLRestriction 적용) 를 통해 활성 참여자를 조회한다.
 */
@Service
class JamReservationSyncService(
    private val jamReservationRepository: JamReservationRepository,
    private val jamParticipantRepository: JamParticipantRepository,
) {
    @Transactional
    fun sync(jam: Jam) {
        // 1. 기존 예약 전량 삭제 (delete-reinsert)
        jamReservationRepository.deleteAllByJamId(jam.id)

        // 2. 소프트 삭제된 Jam 이면 예약 삭제만 하고 종료
        if (jam.deletedAt != null) return

        // 3. 활성 참여자의 중복 제거된 멤버 목록
        val distinctMembers =
            jamParticipantRepository
                .findAllByJam(jam)
                .map { it.member }
                .distinct()
        if (distinctMembers.isEmpty()) return

        // 4. Jam 시간 구간 계산
        val startAt = jam.timeInfo.startAt
        val endAt = startAt.plusMinutes(jam.timeInfo.durationMinutes.toLong())

        // 5. 멤버별 예약 생성
        val reservations =
            distinctMembers.map { memberId ->
                JamReservation.create(
                    memberId = memberId,
                    jamId = jam.id,
                    startAt = startAt,
                    endAt = endAt,
                )
            }
        jamReservationRepository.saveAll(reservations)
    }
}
