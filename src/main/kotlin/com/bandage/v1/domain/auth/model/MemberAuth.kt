package com.bandage.v1.domain.auth.model

import com.bandage.v1.global.common.domain.BaseTimeEntity
import com.bandage.v1.global.common.domain.enums.MemberRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "p_member_auth")
@SQLRestriction("deleted_at IS NULL")
open class MemberAuth(
    id: Long,
    email: String,
    password: String,
    role: MemberRole = MemberRole.MEMBER,
) : BaseTimeEntity() {
    @Id
    @Column(name = "member_id")
    val id: Long = id

    @Column(name = "email", unique = true, nullable = false)
    val email: String = email

    @Column(name = "password", nullable = false)
    val password: String = password

    @Column(name = "role", nullable = false)
    val role: MemberRole = role

    companion object {
        fun create(
            id: Long,
            email: String,
            password: String,
            role: MemberRole,
        ): MemberAuth =
            MemberAuth(
                id = id,
                email = email,
                password = password,
                role = role,
            )
    }
}
