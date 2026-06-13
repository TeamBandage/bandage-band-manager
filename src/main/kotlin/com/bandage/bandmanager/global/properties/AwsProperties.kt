package com.bandage.bandmanager.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
    val credentials: Credentials,
    val region: Region,
    val s3: S3,
    val cloudfront: CloudFront,
) {
    data class Credentials(
        val accessKey: String,
        val secretKey: String,
    )

    data class Region(
        val static: String,
    )

    data class S3(
        val bucket: String,
    )

    data class CloudFront(
        val domain: String,
    )
}
