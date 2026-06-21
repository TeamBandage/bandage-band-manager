package com.bandage.bandmanager.global.infra.s3

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * 이미지 업로드 presigned URL 발급 공통 처리.
 * content-type/확장자/용량 검증 후 `{keyPrefix}/{uuid}.{ext}` 키로 presigned PUT URL 을 발급한다.
 * 권한 검증과 키 prefix 결정은 각 도메인(밴드/멤버/공연)이 소유한다.
 */
@Component
class ImagePresignSupport(
    private val s3PresignedUrlIssuer: S3PresignedUrlIssuer,
) {
    fun issue(
        request: ImagePresignRequest,
        keyPrefix: String,
    ): ImagePresignResponse {
        val normalizedContentType = request.contentType.trim().lowercase()
        val normalizedExt =
            request.ext
                .trim()
                .lowercase()
                .removePrefix(".")
        validateContentTypeAndExt(normalizedContentType, normalizedExt)
        validateContentLength(request.contentLength)

        val objectKey = "${keyPrefix.trimEnd('/')}/${UUID.randomUUID()}.$normalizedExt"
        val uploadUrl = s3PresignedUrlIssuer.issuePutUrl(objectKey, normalizedContentType, request.contentLength)
        return ImagePresignResponse(
            uploadUrl = uploadUrl,
            objectKey = objectKey,
            expiresInSeconds = S3PresignedUrlIssuer.DEFAULT_TTL.seconds,
        )
    }

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
        if (contentLength <= 0 || contentLength > MAX_IMAGE_BYTES) {
            throw BusinessException(ErrorCode.FILE_SIZE_EXCEEDED)
        }
    }

    companion object {
        private const val MAX_IMAGE_BYTES: Long = 5L * 1024 * 1024
        private val ALLOWED_CONTENT_TYPE_TO_EXTS: Map<String, Set<String>> =
            mapOf(
                "image/jpeg" to setOf("jpg", "jpeg"),
                "image/png" to setOf("png"),
                "image/webp" to setOf("webp"),
            )
    }
}
