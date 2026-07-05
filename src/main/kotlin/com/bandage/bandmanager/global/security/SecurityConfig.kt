package com.bandage.bandmanager.global.security

import com.bandage.bandmanager.global.security.constants.SecurityPathConstants
import com.bandage.bandmanager.global.security.filter.JwtAuthenticationFilter
import com.bandage.bandmanager.global.security.handler.JwtAuthenticationEntryPoint
import com.bandage.bandmanager.global.security.jwt.JwtProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
) {
    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        *SecurityPathConstants.AUTH_WHITELIST,
                        *SecurityPathConstants.METRICS,
                        *SecurityPathConstants.SWAGGER_PATHS,
                        *SecurityPathConstants.TMP_FOR_TEST,
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .addFilterBefore(JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns =
                    listOf(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://bandage.team",
                        "https://bandage.team/swagger-ui/index.html",
                    )
                allowedMethods =
                    listOf(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name(),
                    )
                allowedHeaders = listOf(CorsConfiguration.ALL)
                exposedHeaders = listOf(HttpHeaders.SET_COOKIE)
                allowCredentials = true
                maxAge = 3600
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
