package com.bandage.bandmanager.global.jpa

import com.bandage.bandmanager.global.security.PrincipalDetails
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class MemberAuditorAware : AuditorAware<Long> {
    override fun getCurrentAuditor(): Optional<Long> {
        val eventAuditor = AuditContextHolder.getAuditor()
        if (eventAuditor != null) return Optional.of(eventAuditor)

        val auth = SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth.principal !is String) {
            val memberId = (auth.principal as PrincipalDetails).memberId
            return Optional.of(memberId.toLong())
        }
        return Optional.of(0L)
    }
}
