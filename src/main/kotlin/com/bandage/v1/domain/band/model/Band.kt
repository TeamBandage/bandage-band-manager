package com.bandage.v1.domain.band.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_band")
class Band(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "member_cnt", nullable = false)
    var memberCnt: Int = 1,
    @Column(name = "profile_img")
    var profileImg: String? = null,
) : BaseEntity() {
    @Id
    @Column(name = "band_id", nullable = false)
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    val id: UUID? = null

    companion object {
        fun create(
            name: String,
            profileImg: String?,
        ): Band =
            Band(
                name = name,
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
