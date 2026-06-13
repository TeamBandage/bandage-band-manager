package com.bandage.bandmanager.global.infra.s3

import com.bandage.bandmanager.global.properties.AwsProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

@Component
class S3PresignedUrlIssuer(
    private val s3Presigner: S3Presigner,
    private val awsProperties: AwsProperties,
) {
    fun issuePutUrl(
        objectKey: String,
        contentType: String,
        contentLength: Long,
        ttl: Duration = DEFAULT_TTL,
    ): String {
        val putRequest =
            PutObjectRequest
                .builder()
                .bucket(awsProperties.s3.bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build()
        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(ttl)
                .putObjectRequest(putRequest)
                .build()
        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    companion object {
        val DEFAULT_TTL: Duration = Duration.ofMinutes(5)
    }
}
