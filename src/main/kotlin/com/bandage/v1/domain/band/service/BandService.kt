package com.bandage.v1.domain.band.service

import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.global.common.exception.errorcode.ErrorCode
import com.bandage.v1.global.common.exception.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BandService(
    private val bandRepository: BandRepository,
) {
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
}
