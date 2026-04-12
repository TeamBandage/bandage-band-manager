package com.bandage.v1.domain.practice.service

import com.bandage.v1.domain.practice.dto.req.PracticeCreateRequest
import com.bandage.v1.domain.practice.dto.res.PracticeResponse
import com.bandage.v1.domain.practice.model.Practice
import com.bandage.v1.domain.practice.model.PracticeSong
import com.bandage.v1.domain.practice.repository.PracticeParticipantRepository
import com.bandage.v1.domain.practice.repository.PracticeRepository
import com.bandage.v1.domain.practice.repository.PracticeSessionRepository
import com.bandage.v1.domain.practice.repository.PracticeSongRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PracticeService(
    private val practiceRepository: PracticeRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
    private val practiceParticipantRepository: PracticeParticipantRepository,
    private val practiceSongRepository: PracticeSongRepository,
) {
    @Transactional
    fun createPractice(request: PracticeCreateRequest): PracticeResponse {
        val song = getPracticeSong(request.song)
        val practice =
            practiceRepository.save(
                Practice.create(
                    title = request.title ?: song.title,
                    song = song,
                    venue = request.venue,
                    startAt = request.startAt,
                    durationMinutes = request.durationMinutes,
                ),
            )
        return PracticeResponse.of(practice)
    }

    private fun getPracticeSong(songId: UUID): PracticeSong =
        practiceSongRepository.getPracticeSongById(songId)
            ?: throw BusinessException(ErrorCode.PRACTICE_SONG_NOT_FOUND)
}
