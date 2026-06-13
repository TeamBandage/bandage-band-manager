package com.bandage.bandmanager.global.util

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

class SecurityUtil {
    companion object {
        fun getCurrentMemberId(): Long {
            val userDetails =
                SecurityContextHolder.getContext().authentication?.principal as? UserDetails
                    ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
            return userDetails.username.toLong()
        }
    }
}
