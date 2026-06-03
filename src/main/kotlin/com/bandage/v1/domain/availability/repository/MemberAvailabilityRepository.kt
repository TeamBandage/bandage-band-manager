package com.bandage.v1.domain.availability.repository

import com.bandage.v1.domain.availability.model.MemberAvailability
import org.springframework.data.jpa.repository.JpaRepository

interface MemberAvailabilityRepository : JpaRepository<MemberAvailability, Long> {
    fun findByMemberId(memberId: Long): MemberAvailability?

    fun findAllByMemberIdIn(memberIds: Collection<Long>): List<MemberAvailability>
}
