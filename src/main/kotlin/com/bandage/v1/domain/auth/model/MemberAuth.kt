package com.bandage.v1.domain.auth.model

import com.bandage.v1.domain.auth.model.enums.MemberRole
import com.bandage.v1.global.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "p_member_auth")
@SQLRestriction("deleted_at IS NULL")
open class MemberAuth(
    memberId: Long,
    email: String,
    password: String,
    role: MemberRole = MemberRole.MEMBER,
) : BaseTimeEntity() {
    @Id
    @Column(name = "member_id", unique = true, nullable = false)
    val memberId: Long = memberId

    @Column(name = "email", unique = true, nullable = false)
    val email: String = email

    @Column(name = "password", nullable = false)
    var password: String = password

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: MemberRole = role

    companion object {
        fun create(
            memberId: Long,
            email: String,
            password: String,
        ): MemberAuth =
            MemberAuth(
                memberId = memberId,
                email = email,
                password = password,
            )
    }

    fun updatePassword(newPassword: String) {
        this.password = newPassword
    }
}
