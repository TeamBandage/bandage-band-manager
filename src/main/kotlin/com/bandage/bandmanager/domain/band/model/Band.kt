package com.bandage.bandmanager.domain.band.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_band")
@SQLRestriction("deleted_at IS NULL")
open class Band(
    name: String,
    description: String,
    profileImg: String?,
) : BaseEntity() {
    @Id
    @Column(name = "band_id", nullable = false)
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "name", unique = true, nullable = false)
    var name: String = name
        protected set

    @Lob
    @Column(name = "description", nullable = false)
    var description: String? = description
        protected set

    @Column(name = "member_cnt", nullable = false)
    var memberCnt: Int = 1
        protected set

    @Column(name = "profile_img")
    var profileImg: String? = profileImg
        protected set

    companion object {
        fun create(
            name: String,
            description: String,
            profileImg: String?,
        ): Band =
            Band(
                name = name,
                description = description,
                profileImg = profileImg,
            )
    }

    fun addMemberCnt() {
        this.memberCnt++
    }

    fun decreaseMemberCnt() {
        if (this.memberCnt > 0) this.memberCnt--
    }

    fun updateImg(newImg: String) {
        this.profileImg = newImg
    }

    fun deleteImg() {
        this.profileImg = null
    }

    fun updateName(newName: String) {
        this.name = newName
    }

    fun updateDescription(newDescription: String?) {
        this.description = newDescription
    }
}
