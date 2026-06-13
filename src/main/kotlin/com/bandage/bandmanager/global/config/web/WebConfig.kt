package com.bandage.bandmanager.global.config.web

import com.bandage.bandmanager.global.security.annotation.CurrentMemberIdResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val currentMemberIdResolver: CurrentMemberIdResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentMemberIdResolver)
    }
}
