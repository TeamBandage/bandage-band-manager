package com.bandage.bandmanager.global.infra.s3

import com.bandage.bandmanager.global.properties.AwsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config(
    private val awsProperties: AwsProperties,
) {
    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials =
            AwsBasicCredentials.create(
                awsProperties.credentials.accessKey,
                awsProperties.credentials.secretKey,
            )
        return S3Presigner
            .builder()
            .region(Region.of(awsProperties.region.static))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}
