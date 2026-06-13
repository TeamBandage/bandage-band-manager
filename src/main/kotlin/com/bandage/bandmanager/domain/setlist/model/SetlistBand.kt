package com.bandage.bandmanager.domain.setlist.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_setlist_band")
@SQLRestriction("deleted_at IS NULL")
open class SetlistBand(
    bandId: UUID,
    setlistId: UUID,
) : BaseEntity() {
    @Id
    @Column(name = "setlist_band_id", nullable = false, updatable = false)
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "band_id", nullable = false)
    var bandId: UUID = bandId
        protected set

    @Column(name = "setlist_id", nullable = false)
    var setlistId: UUID = setlistId
        protected set

    companion object {
        fun create(
            bandId: UUID,
            setlistId: UUID,
        ): SetlistBand =
            SetlistBand(
                bandId = bandId,
                setlistId = setlistId,
            )
    }
}
