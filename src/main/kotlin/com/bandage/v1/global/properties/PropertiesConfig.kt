package com.bandage.v1.global.properties

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    value = [
        JwtProperties::class,
        AwsProperties::class,
        KakaoOAuthProperties::class,
    ],
)
class PropertiesConfig
