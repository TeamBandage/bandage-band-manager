package com.bandage.bandmanager.global.infra.s3

import com.bandage.bandmanager.global.properties.AwsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config(
    private val awsProperties: AwsProperties,
) {
    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner
            .builder()
            .region(Region.of(awsProperties.region.static))
            .credentialsProvider(StaticCredentialsProvider.create(credentials()))
            .build()

    /** 업로드된 객체의 존재 확인(headObject)용. presigned URL 발급은 [s3Presigner] 가 담당한다. */
    @Bean
    fun s3Client(): S3Client =
        S3Client
            .builder()
            .region(Region.of(awsProperties.region.static))
            .credentialsProvider(StaticCredentialsProvider.create(credentials()))
            .build()

    private fun credentials(): AwsBasicCredentials =
        AwsBasicCredentials.create(
            awsProperties.credentials.accessKey,
            awsProperties.credentials.secretKey,
        )
}
