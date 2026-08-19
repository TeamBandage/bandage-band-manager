package com.bandage.bandmanager.domain.member.repository

import com.bandage.bandmanager.domain.member.model.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository :
    JpaRepository<Member, Long>,
    MemberRepositoryCustom {
    fun existsByEmail(email: String): Boolean

    fun findAllByIdIn(ids: Collection<Long>): List<Member>
}
