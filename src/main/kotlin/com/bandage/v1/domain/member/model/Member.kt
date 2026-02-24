package com.bandage.v1.domain.member.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.util.UUID

@Entity
@Table(name = "p_member")
@SQLRestriction("deleted_at IS NULL")
open class Member(
    @Column(name = "email", nullable = false)
    var email: String,
    @Column(name = "password", nullable = false)
    var password: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "contact", nullable = false)
    var contact: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    lateinit var id: UUID
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
