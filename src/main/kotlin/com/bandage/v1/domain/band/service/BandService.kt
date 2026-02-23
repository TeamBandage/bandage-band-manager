package com.bandage.v1.domain.band.service

import com.bandage.v1.domain.band.dto.req.BandCreateRequest
import com.bandage.v1.domain.band.dto.res.BandResponse
import com.bandage.v1.domain.band.model.Band
import com.bandage.v1.domain.band.repository.BandRepository
import org.springframework.stereotype.Service

@Service
class BandService(
    private val bandRepository: BandRepository,
) {
    fun createBand(request: BandCreateRequest): BandResponse {
        val band =
            bandRepository.save(
                Band.create(
                    name = request.name,
                    profileImg = request.profileImg,
                ),
            )
        return BandResponse.of(band)
    }
}
