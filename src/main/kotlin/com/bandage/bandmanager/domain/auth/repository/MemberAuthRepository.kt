package com.bandage.bandmanager.domain.auth.repository

import com.bandage.bandmanager.domain.auth.model.MemberAuth
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberAuthRepository : JpaRepository<MemberAuth, Long> {
    fun findByEmail(email: String): MemberAuth?

    fun findByMemberId(memberId: Long): MemberAuth?

    fun existsByMemberId(memberId: Long): Boolean
}
