package com.bandage.bandmanager.domain.selection.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TrackInfo
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_track_selection_item")
@SQLRestriction("deleted_at IS NULL")
open class TrackSelectionItem(
    selection: TrackSelection,
    trackInfo: TrackInfo,
    proposerId: Long,
    note: String?,
) : BaseEntity() {
    @Id
    @Column(name = "track_selection_item_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_selection_id", nullable = false)
    val selection: TrackSelection = selection

    @Embedded
    var trackInfo: TrackInfo = trackInfo
        protected set

    @Column(name = "proposer_id", nullable = false)
    val proposerId: Long = proposerId

    @Column(name = "note", nullable = true, length = 1000)
    var note: String? = note
        protected set

    @Column(name = "is_selected", nullable = false)
    var isSelected: Boolean = false
        protected set

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_track_selection_item_session",
        joinColumns = [JoinColumn(name = "track_selection_item_id")],
    )
    private var _sessions: MutableList<SessionDef> = mutableListOf()

    val sessions: List<SessionDef> get() = _sessions.toList()

    companion object {
        fun create(
            selection: TrackSelection,
            trackInfo: TrackInfo,
            proposerId: Long,
            note: String?,
            sessions: List<SessionDef>,
        ): TrackSelectionItem =
            TrackSelectionItem(
                selection = selection,
                trackInfo = trackInfo,
                proposerId = proposerId,
                note = note,
            ).apply {
                _sessions.addAll(sessions)
            }
    }

    fun updateMeta(
        title: String?,
        artist: String?,
        album: String?,
        duration: Int?,
        reference: String?,
        note: String?,
    ) {
        trackInfo.update(title, artist, album, duration, reference)
        note?.let { this.note = it }
    }

    fun replaceSessions(newSessions: List<SessionDef>) {
        this._sessions.clear()
        this._sessions.addAll(newSessions)
    }

    fun select() {
        this.isSelected = true
    }

    fun deselect() {
        this.isSelected = false
    }
}
