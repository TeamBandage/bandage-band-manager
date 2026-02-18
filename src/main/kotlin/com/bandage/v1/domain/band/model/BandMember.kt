package com.bandage.v1.domain.band.model

import com.bandage.v1.domain.member.model.Member
import com.bandage.v1.global.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "p_band_member")
class BandMember(
    @JoinColumn(name = "band_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val band: Band,

    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val member: Member,

    @Column(name = "role", nullable = false)
    var role: BandRole = BandRole.MEMBER
): BaseEntity() {
    @Id
    @Column(name = "band_member_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    companion object {
        fun create(band: Band, member: Member): BandMember {
            return BandMember(
                band = band,
                member = member
            )
        }
    }
    fun promoteToLeader() {
        require(this.role != BandRole.LEADER)
        this.role = BandRole.LEADER
    }
    fun dismissFromLeader() {
        require(this.role == BandRole.LEADER)
        this.role = BandRole.MEMBER
    }
}