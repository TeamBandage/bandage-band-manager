package com.bandage.v1.domain.member.domain

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
    var memberEmail: String,
    @Column(name = "member_password", nullable = false)
    var memberPassword: String,
    @Column(name = "member_name", nullable = false)
    var memberName: String,
    @Column(name = "member_contact", nullable = false)
    var memberContact: String,
    ): BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    val memberId: Long? = null

    companion object {
        fun create(email: String, password: String, name: String, contact: String): Member {
            return Member(email, password, name, contact)
        }
    }
    fun updateName(newName: String) {
        this.memberName = newName
    }
    fun updateContact(newContact: String) {
        this.memberContact = newContact
    }
}