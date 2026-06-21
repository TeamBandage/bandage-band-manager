package com.bandage.bandmanager.domain.member.model

import com.bandage.bandmanager.global.common.domain.BaseTimeEntity
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
    name: String,
    profileImg: String? = null,
) : BaseTimeEntity() {
    @Id
    @Column(name = "member_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    // 활성 회원 간 email 유일성은 partial unique index(uk_member_email_active, Liquibase 017)로 관리
    @Column(name = "email", nullable = false)
    var email: String = email
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "profile_img")
    var profileImg: String? = profileImg
        protected set

    companion object {
        fun create(
            email: String,
            name: String,
            profileImg: String? = null,
        ): Member =
            Member(
                email = email,
                name = name,
                profileImg = profileImg,
            )

        fun createOAuth(
            email: String,
            name: String,
            profileImg: String? = null,
        ): Member =
            Member(
                email = email,
                name = name,
                profileImg = profileImg,
            )
    }

    fun updateName(newName: String) {
        this.name = newName
    }

    fun updateProfileImg(newProfileImg: String?) {
        this.profileImg = newProfileImg
    }
}
