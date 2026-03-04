package com.bandage.v1.domain.member.repository

import com.bandage.v1.domain.member.model.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?

    fun existsMemberByEmail(email: String): Boolean
}
