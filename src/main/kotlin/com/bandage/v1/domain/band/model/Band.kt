package com.bandage.v1.domain.band.model

import com.bandage.v1.global.common.domain.BaseEntity
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
    @Column(name = "name", nullable = false)
    var name: String,
    @Lob
    @Column(name = "description", nullable = false)
    var description: String? = null,
    @Column(name = "member_cnt", nullable = false)
    var memberCnt: Int = 1,
    @Column(name = "profile_img")
    var profileImg: String? = null,
) : BaseEntity() {
    @Id
    @Column(name = "band_id", nullable = false)
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
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

    fun updateImg(newImg: String) {
        this.profileImg = newImg
    }
}
