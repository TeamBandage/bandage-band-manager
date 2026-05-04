package com.bandage.v1.global.infra.s3

import com.bandage.v1.global.properties.AwsProperties
import org.springframework.stereotype.Component

@Component
class CloudFrontUrlResolver(
    awsProperties: AwsProperties,
) {
    private val baseUrl: String =
        awsProperties.cloudfront.domain
            .trim()
            .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
            .trimEnd('/')

    fun resolve(objectKey: String): String = "$baseUrl/${objectKey.trimStart('/')}"

    fun resolveOrNull(objectKey: String?): String? = objectKey?.takeIf { it.isNotBlank() }?.let { resolve(it) }
}
