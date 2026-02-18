package com.bandage.v1.domain.member.model

import com.bandage.v1.global.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "p_member")
class Member (
    @Column(name = "member_email", nullable = false)
    var email: String,
    @Column(name = "member_password", nullable = false)
    var password: String,
    @Column(name = "member_name", nullable = false)
    var name: String,
    @Column(name = "member_contact", nullable = false)
    var contact: String,
    ): BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    val id: Long? = null

    companion object {
        fun create(email: String, password: String, name: String, contact: String): Member {
            return Member(
                email = email,
                password = password,
                name = name,
                contact = contact
            )
        }
    }
    fun updateName(newName: String) {
        this.name = newName
    }
    fun updateContact(newContact: String) {
        this.contact = newContact
    }
}