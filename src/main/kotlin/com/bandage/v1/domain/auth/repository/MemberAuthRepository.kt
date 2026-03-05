package com.bandage.v1.domain.auth.repository

import com.bandage.v1.domain.auth.model.MemberAuth
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberAuthRepository : JpaRepository<MemberAuth, Long> {
    fun findByEmail(email: String): MemberAuth?

    fun findByMemberId(memberId: Long): MemberAuth?
}
