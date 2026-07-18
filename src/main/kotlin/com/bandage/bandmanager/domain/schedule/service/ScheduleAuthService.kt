package com.bandage.bandmanager.domain.schedule.service

import com.bandage.bandmanager.domain.setlist.model.Setlist
import com.bandage.bandmanager.domain.setlist.repository.SetlistRepository
import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 시간표(schedule) 도메인의 setlist 스코프 권한 검증.
 *
 * - 매니저: setlist.managerId == memberId
 * - 참여자(access): 매니저이거나 setlist 접근 권한 보유 회원
 */
@Service
@Transactional(readOnly = true)
class ScheduleAuthService(
    private val setlistRepository: SetlistRepository,
) {
    fun getSetlistOrThrow(setlistId: UUID): Setlist =
        setlistRepository.findByIdOrNull(setlistId)
            ?: throw BusinessException(ErrorCode.SETLIST_NOT_FOUND)

    fun isSetlistManager(
        setlistId: UUID,
        memberId: Long,
    ): Boolean = getSetlistOrThrow(setlistId).managerId == memberId

    fun isSetlistParticipant(
        setlistId: UUID,
        memberId: Long,
    ): Boolean {
        val setlist = getSetlistOrThrow(setlistId)
        if (setlist.managerId == memberId) return true
        return setlistRepository.isAccessibleMember(setlistId, memberId)
    }

    fun validateSetlistManager(
        setlistId: UUID,
        memberId: Long,
    ) {
        if (!isSetlistManager(setlistId, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_NOT_MANAGER)
        }
    }

    fun validateSetlistParticipant(
        setlistId: UUID,
        memberId: Long,
    ) {
        if (!isSetlistParticipant(setlistId, memberId)) {
            throw BusinessException(ErrorCode.SETLIST_FORBIDDEN)
        }
    }
}
