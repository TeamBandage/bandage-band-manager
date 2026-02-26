package com.bandage.v1.global.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        *SecurityPathConstants.METRICS,
                        *SecurityPathConstants.SWAGGER_PATHS,
                        *SecurityPathConstants.AUTH_WHITELIST,
                        *SecurityPathConstants.TMP_FOR_TEST,
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }
}
