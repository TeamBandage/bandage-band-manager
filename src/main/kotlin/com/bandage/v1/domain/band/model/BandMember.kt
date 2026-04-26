package com.bandage.v1.domain.band.model

import com.bandage.v1.domain.band.model.enums.BandRole
import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_band_member")
@SQLRestriction("deleted_at IS NULL")
open class BandMember(
    band: Band,
    member: Long,
    role: BandRole = BandRole.MEMBER,
) : BaseEntity() {
    @Id
    @Column(name = "band_member_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @JoinColumn(name = "band_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    val band: Band = band

    @Column(name = "member_id", nullable = false)
    val member: Long = member

    @Column(name = "role", nullable = false)
    var role: BandRole = role
        protected set

    companion object {
        fun create(
            band: Band,
            member: Long,
            role: BandRole,
        ): BandMember =
            BandMember(
                band = band,
                member = member,
                role = role,
            )
    }

    fun promoteToLeader() {
        require(this.role != BandRole.LEADER)
        this.role = BandRole.LEADER
    }

    fun dismissFromLeader() {
        require(this.role == BandRole.LEADER)
        this.role = BandRole.MEMBER
    }

    fun changeRole(newRole: BandRole) {
        require(this.role != BandRole.LEADER) { "리더의 역할 변경은 위임 API 를 사용해야 합니다." }
        require(newRole != BandRole.LEADER) { "리더로의 승격은 위임 API 를 사용해야 합니다." }
        this.role = newRole
    }
}
