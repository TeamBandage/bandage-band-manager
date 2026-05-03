package com.bandage.v1.domain.upload.service

import com.bandage.v1.domain.band.model.enums.BandRole
import com.bandage.v1.domain.band.repository.BandMemberRepository
import com.bandage.v1.domain.band.repository.BandRepository
import com.bandage.v1.domain.upload.dto.req.ProfileImagePresignRequest
import com.bandage.v1.domain.upload.dto.res.ProfileImagePresignResponse
import com.bandage.v1.domain.upload.model.enums.UploadDomain
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import com.bandage.v1.global.infra.s3.S3PresignedUrlIssuer
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProfileImageUploadService(
    private val s3PresignedUrlIssuer: S3PresignedUrlIssuer,
    private val bandRepository: BandRepository,
    private val bandMemberRepository: BandMemberRepository,
) {
    fun issuePresignedUrl(
        request: ProfileImagePresignRequest,
        memberId: Long,
    ): ProfileImagePresignResponse {
        val contentType = normalizeContentType(request.contentType)
        val ext = normalizeExt(request.ext)
        validateContentTypeAndExt(contentType, ext)
        validateContentLength(request.contentLength)

        val objectKey =
            when (request.domain) {
                UploadDomain.BAND -> {
                    val bandId = request.bandId ?: throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
                    validateBandLeader(bandId, memberId)
                    bandProfileKey(bandId, ext)
                }
                UploadDomain.MEMBER -> memberProfileKey(memberId, ext)
            }

        val uploadUrl = s3PresignedUrlIssuer.issuePutUrl(objectKey, contentType, request.contentLength)
        return ProfileImagePresignResponse(
            uploadUrl = uploadUrl,
            objectKey = objectKey,
            expiresInSeconds = S3PresignedUrlIssuer.DEFAULT_TTL.seconds,
        )
    }

    private fun normalizeContentType(raw: String): String = raw.trim().lowercase()

    private fun normalizeExt(raw: String): String = raw.trim().lowercase().removePrefix(".")

    private fun validateContentTypeAndExt(
        contentType: String,
        ext: String,
    ) {
        val allowedExts =
            ALLOWED_CONTENT_TYPE_TO_EXTS[contentType]
                ?: throw BusinessException(ErrorCode.INVALID_FILE_CONTENT_TYPE)
        if (ext !in allowedExts) throw BusinessException(ErrorCode.INVALID_FILE_EXTENSION)
    }

    private fun validateContentLength(contentLength: Long) {
        if (contentLength <= 0 || contentLength > MAX_PROFILE_IMAGE_BYTES) {
            throw BusinessException(ErrorCode.FILE_SIZE_EXCEEDED)
        }
    }

    private fun validateBandLeader(
        bandId: UUID,
        memberId: Long,
    ) {
        val band =
            bandRepository.findByIdOrNull(bandId)
                ?: throw BusinessException(ErrorCode.BAND_NOT_FOUND)
        if (!bandMemberRepository.existsByBandAndMemberAndRole(band, memberId, BandRole.LEADER)) {
            throw BusinessException(ErrorCode.NOT_A_LEADER)
        }
    }

    private fun bandProfileKey(
        bandId: UUID,
        ext: String,
    ): String = "profile/band/$bandId/${UUID.randomUUID()}.$ext"

    private fun memberProfileKey(
        memberId: Long,
        ext: String,
    ): String = "profile/member/$memberId/${UUID.randomUUID()}.$ext"

    companion object {
        private const val MAX_PROFILE_IMAGE_BYTES: Long = 5L * 1024 * 1024
        private val ALLOWED_CONTENT_TYPE_TO_EXTS: Map<String, Set<String>> =
            mapOf(
                "image/jpeg" to setOf("jpg", "jpeg"),
                "image/png" to setOf("png"),
                "image/webp" to setOf("webp"),
            )
    }
}
