package com.bandage.v1.domain.band.service

import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.res.BandInfoResponse
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.global.common.dto.CursorResponse
import com.bandage.v1.global.common.exception.errorcode.ErrorCode
import com.bandage.v1.global.common.exception.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BandService(
    private val bandRepository: BandRepository,
) {
    // TODO: profileImg multi-part 처리 구현
    @Transactional
    fun createBand(request: BandCreateRequest): BandResponse {
        if (bandRepository.existsByName(request.name)) {
            throw BusinessException(ErrorCode.DUPLICATE_BAND_NAME)
        }
        val band =
            bandRepository.save(
                Band.create(
                    name = request.name,
                    description = request.description,
                    profileImg = request.profileImg,
                ),
            )
        return BandResponse.of(band)
    }

    fun getBand(bandId: UUID): BandInfoResponse {
        val band =
            bandRepository.findByIdOrNull(bandId)
                ?: throw BusinessException(ErrorCode.BAND_NOT_FOUND)
        return BandInfoResponse.of(band)
    }

    fun getBandsByCursor(
        lastId: UUID?,
        size: Int,
    ): CursorResponse<BandInfoResponse, UUID> {
        val result = bandRepository.findAllByPaging(lastId, size)
        return CursorResponse(
            content = result.content.map { BandInfoResponse.of(it) },
            nextCursor = result.nextCursor,
            hasNext = result.hasNext,
        )
    }
}
