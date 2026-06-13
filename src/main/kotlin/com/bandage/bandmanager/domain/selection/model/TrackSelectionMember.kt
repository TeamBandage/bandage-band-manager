package com.bandage.bandmanager.domain.selection.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
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
    name = "p_track_selection_member",
    uniqueConstraints = [UniqueConstraint(name = "uk_track_selection_member", columnNames = ["track_selection_id", "member_id"])],
)
@SQLRestriction("deleted_at IS NULL")
open class TrackSelectionMember(
    selection: TrackSelection,
    memberId: Long,
    bandIds: Set<UUID> = emptySet(),
) : BaseEntity() {
    @Id
    @Column(name = "track_selection_member_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_selection_id", nullable = false)
    val selection: TrackSelection = selection

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_track_selection_member_band",
        joinColumns = [JoinColumn(name = "track_selection_member_id")],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_track_selection_member_band",
                columnNames = ["track_selection_member_id", "band_id"],
            ),
        ],
    )
    @Column(name = "band_id", nullable = false)
    private val _bandIds: MutableSet<UUID> = bandIds.toMutableSet()

    val bandIds: Set<UUID> get() = _bandIds.toSet()

    fun replaceBandIds(newBandIds: Set<UUID>) {
        _bandIds.clear()
        _bandIds.addAll(newBandIds)
    }

    companion object {
        fun create(
            selection: TrackSelection,
            memberId: Long,
            bandIds: Set<UUID> = emptySet(),
        ): TrackSelectionMember =
            TrackSelectionMember(
                selection = selection,
                memberId = memberId,
                bandIds = bandIds,
            )
    }
}
