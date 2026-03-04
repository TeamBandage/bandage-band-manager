package com.bandage.v1.domain.member.model

import com.bandage.v1.domain.member.model.enums.MemberRole
import com.bandage.v1.global.common.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "p_member")
@SQLRestriction("deleted_at IS NULL")
open class Member(
    email: String,
    password: String,
    name: String,
    contact: String,
    role: MemberRole = MemberRole.MEMBER,
) : BaseTimeEntity() {
    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "email", unique = true, nullable = false)
    var email: String = email
        protected set

    @Column(name = "password", nullable = false)
    var password: String = password
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "contact", nullable = false)
    var contact: String = contact
        protected set

    @Column(name = "role", nullable = false)
    var role: MemberRole = role
        protected set

    companion object {
        fun create(
            email: String,
            password: String,
            name: String,
            contact: String,
        ): Member =
            Member(
                email = email,
                password = password,
                name = name,
                contact = contact,
            )
    }

    fun updateName(newName: String) {
        this.name = newName
    }

    fun updateContact(newContact: String) {
        this.contact = newContact
    }
}
