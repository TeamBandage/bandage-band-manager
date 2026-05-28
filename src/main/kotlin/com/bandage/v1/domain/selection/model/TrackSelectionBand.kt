package com.bandage.v1.domain.selection.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_track_selection_band",
    uniqueConstraints = [UniqueConstraint(name = "uk_track_selection_band", columnNames = ["track_selection_id", "band_id"])],
)
@SQLRestriction("deleted_at IS NULL")
open class TrackSelectionBand(
    selection: TrackSelection,
    bandId: UUID,
) : BaseEntity() {
    @Id
    @Column(name = "track_selection_band_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_selection_id", nullable = false)
    val selection: TrackSelection = selection

    @Column(name = "band_id", nullable = false)
    val bandId: UUID = bandId

    companion object {
        fun create(
            selection: TrackSelection,
            bandId: UUID,
        ): TrackSelectionBand = TrackSelectionBand(selection = selection, bandId = bandId)
    }
}
