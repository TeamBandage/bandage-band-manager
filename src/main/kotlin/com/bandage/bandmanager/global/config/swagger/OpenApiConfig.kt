package com.bandage.bandmanager.global.config.swagger

import com.bandage.bandmanager.global.security.annotation.CurrentMemberId
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@OpenAPIDefinition(
    security = [SecurityRequirement(name = "bearerAuth")],
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
)
@Configuration
@Profile("swagger")
class OpenApiConfig {
    init {
        // @CurrentMemberId 는 JWT(SecurityContext)에서 주입되는 인증 파생값으로,
        // CurrentMemberIdResolver 가 처리한다. SpringDoc 은 이 커스텀 리졸버를 모르기 때문에
        // 해당 파라미터를 query 파라미터로 잘못 노출한다. 스펙에서 제외하여 phantom memberId 를 제거한다.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentMemberId::class.java)
    }

    @Bean
    fun bandageOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Bandage API")
                    .description("밴드 합주 & 공연 관리 플랫폼 Bandage API 명세서")
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("Sunwoo Jung")
                            .email("bandage2026@gmail.com")
                            .url("https://github.com/TeamBandage"),
                    ),
            )
}
