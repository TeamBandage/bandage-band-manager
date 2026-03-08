package com.bandage.v1.global.jpa

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing(auditorAwareRef = "memberAuditorAware")
class JpaConfig {
    @Bean
    fun memberAuditorAware(): AuditorAware<Long> = MemberAuditorAware()
}
